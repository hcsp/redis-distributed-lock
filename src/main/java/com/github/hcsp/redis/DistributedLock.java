package com.github.hcsp.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.lang.management.ManagementFactory;
import java.util.concurrent.Callable;

public class DistributedLock {
    private static Jedis jedis;

    // 测试程序用10个jvm模拟分布式环境，用于区分jvm
    private static String jvmName;

    static {
        jedis = new JedisPool().getResource();
        jvmName = ManagementFactory.getRuntimeMXBean().getName();
    }

    /** The lock name. A lock with same name might be shared in multiple JVMs. */
    private String name;

    public DistributedLock(String name) {
        this.name = name;
    }

    /**
     * Run a given action under lock.
     *
     * @param callable the action to be executed
     * @param <T> return type
     * @return the result
     */
    public <T> T runUnderLock(Callable<T> callable) throws Exception {
        // 1 加锁(value要在10个jvm中保证唯一)
        lock();

        // 2 执行callable
        T callResult = callable.call();

        // 3 解锁
        unlock();

        // 4 返回callable结果
        return callResult;
    }

    /**
     * 加锁
     * @return
     */
    void lock() {
        while (true) {
            Long isSuccess = jedis.setnx(name, jvmName);
            if (isSuccess == 1) {
                // 3秒后key过期->释放锁
                jedis.expire(name, 3);
                return;
            }
        }
    }

    /**
     * 解锁
     */
    private void unlock() {
        while (true) {
            String value = jedis.get(name);
            if (jvmName.equals(value)) {
                jedis.del(name);
                return;
            }
        }
    }

}
