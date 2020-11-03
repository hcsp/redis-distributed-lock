package com.github.hcsp.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.Callable;

public class DistributedLock {
    /**
     * The lock name. A lock with same name might be shared in multiple JVMs.
     */
    private String name;
    private static final long TIME_OUT = 1000;

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
        JedisPool pool = new JedisPool();
        try (Jedis jedis = pool.getResource()) {
            String timestamp = Long.toString(System.currentTimeMillis());
            long time = 0;
            // 获取锁
            while (time < TIME_OUT) {
                long setResult = jedis.setnx(name, timestamp);
                if (setResult == 1) {
                    // 执行
                    T result = callable.call();
                    // 释放锁
                    jedis.del(name);
                    System.out.println("执行完毕...");
                    return result;
                } else {
                    time += 10;
                    Thread.sleep(10);
                }
            }
        }
        System.out.println("获取锁失败超时...");
        return null;
    }


}
