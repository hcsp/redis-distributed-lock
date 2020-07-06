package com.github.hcsp.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.lang.management.ManagementFactory;
import java.util.concurrent.Callable;

public class DistributedLock {
    /**
     * The lock name. A lock with same name might be shared in multiple JVMs.
     */
    private String name;

    // 测试程序用10个jvm模拟分布式环境，用于区分jvm
    private static String jvmName;
    private static Jedis jedis;

    static {
        jedis = new JedisPool().getResource();
        //测试程序中是使用了10个线程，所以这里使用jvm名字作为锁；
        jvmName = ManagementFactory.getRuntimeMXBean().getName();
    }

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

        lock(jedis, name);

        T call = callable.call();

        unlock(jedis, name);

        return call;
    }

    /**
     * 分布式锁，这里先向里面set if not exist;
     * 若是锁被别人拿了就一直循环，联系到乐观锁的概念；
     *
     * @param jedis
     * @return
     */
    void lock(Jedis jedis, String lock) {

        try {
            while (true) {
                Long num = jedis.setnx(lock, jvmName);
                if (num == 1) {
                    // 3秒后key过期->释放锁
                    jedis.expire(name, 3);
                    return;
                }
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    void unlock(Jedis jedis, String lock) {
        try {
            while (true) {
                String lockName = jedis.get(lock);
                if (lockName.equals(jvmName)) {
                    System.out.println("Unlock!");
                    jedis.del("lock");
                    return;
                }
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
