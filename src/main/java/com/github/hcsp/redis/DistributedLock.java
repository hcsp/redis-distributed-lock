package com.github.hcsp.redis;

import redis.clients.jedis.Jedis;

import java.util.concurrent.Callable;

public class DistributedLock implements DistributedLockQueue {
    /**
     * The lock name. A lock with same name might be shared in multiple JVMs.
     */
    private String name;

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
        initSubscriber();
        final Jedis jedis = new Jedis();
        while (true) {
            // 获得锁
            final Long lock = jedis.setnx(name, "1");
            // 执行任务
            if (lock == 1) {
                // 执行后释放锁
                final T call;
                try {
                    call = callable.call();
                    return call;
                } finally {
                    jedis.del(name);
                    jedis.publish("DistributedLock", "unlock");
                }
            }

            // 没有获得锁则等待
            poll();
        }

    }

    private void initSubscriber() {
        new DistributedLockSubscriber();
    }

}
