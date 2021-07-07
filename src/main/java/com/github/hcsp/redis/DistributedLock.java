package com.github.hcsp.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;
import java.util.concurrent.Callable;

public class DistributedLock {
    /** The lock name. A lock with same name might be shared in multiple JVMs. */
    private String name;
    private Jedis jedis = new JedisPool().getResource();

    public DistributedLock(String name) {
        this.name = name;
    }

    /**
     * Run a given action under lock.
     *
     * @param callable the action to be executed
     * @param <T>      return type
     * @return the result
     */
    public <T> T runUnderLock(Callable<T> callable) throws Exception {
        String uuid = lock();
        try {
            return callable.call();
        } finally {
            unlock(uuid);
        }

    }

    private void unlock(String uuid) {
        String value = jedis.get(name);
        if (uuid.equals(value)) {
            jedis.del(name);
            jedis.publish("unlock", "has lock");
        }
    }

    private String lock() {
        while (true) {
            String uuid = UUID.randomUUID().toString();
            Long result = jedis.setnx(name, uuid);
            if (result == 1) {
                jedis.expire(name, 5);
                return uuid;
            }
            jedis.subscribe(new DistributedLockSubscriber(System.getProperty("jvmName")), "unlock");
        }
    }

    static class DistributedLockSubscriber extends JedisPubSub {
        private String name;

        public DistributedLockSubscriber(String name) {
            this.name = name;
        }

        @Override
        public void onMessage(String channel, String message) {
            unsubscribe();
        }
    }




}
