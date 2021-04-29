package com.github.hcsp.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

public class DistributedLock {
    /**
     * The lock name. A lock with same name might be shared in multiple JVMs.
     */
    private String name;
    private String uuid;

    private static JedisPool jedisPool;

    static {
        jedisPool = new JedisPool();
    }

    public DistributedLock(String name) {
        this.name = name;
        this.uuid = UUID.randomUUID().toString();
    }

    /**
     * Run a given action under lock.
     *
     * @param callable the action to be executed
     * @param <T>      return type
     * @return the result
     */
    public <T> T runUnderLock(Callable<T> callable) throws Exception {
        try (Jedis jedis = jedisPool.getResource()) {
            try {
                lock(jedis, 1);
                return callable.call();
            } finally {
                unlock(jedis);
            }
        }
    }

    private void addSubscriber(CountDownLatch latch) {
        Thread thread = new Thread(() -> new Jedis().subscribe(new Subscriber(latch), Subscriber.CHANNEL));
        thread.setDaemon(true);
        thread.start();
    }

    public String lock(Jedis jedis, int second) {
        while (true) {
            Long success = jedis.setnx(name, uuid);
            if (success == 1 && second > 0) {
                jedis.expire(name, second);
                return uuid;
            }

            CountDownLatch latch = new CountDownLatch(1);
            addSubscriber(latch);
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void unlock(Jedis jedis) {
        while (true) {
            String s = jedis.get(name);
            if (s.equals(uuid)) {
                jedis.del(name);
                jedis.publish(Subscriber.CHANNEL, Subscriber.UNLOCK);
                break;
            }
        }
    }

    static class Subscriber extends JedisPubSub {
        public static final String CHANNEL = "UNLOCK";
        public static final String UNLOCK = "UNLOCK";
        private CountDownLatch latch;

        public Subscriber(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onMessage(String channel, String message) {
            if (CHANNEL.equals(channel) && UNLOCK.equals(message)) {
                latch.countDown();
            }
        }
    }
}
