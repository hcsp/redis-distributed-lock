package com.github.hcsp.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class TestMain {
    public static void main(String[] args) throws Exception {
        JedisPool pool = new JedisPool();

        new DistributedLock("lock")
                .runUnderLock(
                        () -> {
                            try (Jedis jedis = pool.getResource()) {
                                // try lock
                                tryLock(jedis);
                                String value = jedis.get("KeyUnderTest");
                                if (value == null) {
                                    value = "1";
                                } else {
                                    value = "" + (Integer.parseInt(value) + 1);
                                }
                                jedis.set("KeyUnderTest", value);

                                // unlock
                                unlock(jedis);
                                return null;
                            }
                        });
    }

    private static void unlock(Jedis jedis) {
        jedis.del("distribute_jvm_share_key");
    }

    private static void tryLock(Jedis jedis) {
        while (jedis.setnx("distribute_jvm_share_key", "0") == 0L) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100L));
            System.out.println(Thread.currentThread().getName() + "try to get lock");
        }
    }
}
