package com.github.hcsp.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.lang.management.ManagementFactory;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

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
     * 这里需要多补充两句，再复习的时候，卡壳了，首先是jedis的处理方法没有吃透；
     * 在jedis.setnx的时候，是能拿到lock的就进入if里面，然后就return了
     * 没拿到的就进入下面的countDownLatch方法，等待
     * 之所以循环，是因为设计思路的问题；
     * 结合看的blog,锁通常是有两种需求；
     * ********************************
     * 同样需要使用锁，动机可能完全相反：
     * • 在保证线程安全的前提下，尽量让所有线程都执行成功
     * • 在保证线程安全的前提下，只让一个线程执行成功
     * *********************************
     * 在本场景中的例子是第一种，是需要让所有线程都执行成功
     * 在blog中的定时任务是第二种，同一时间只需要多节点中一个执行成功，避免重复执行；
     * 在blog中定时锁，tryLock就没有用while循环；
     *
     * @param jedis
     * @return
     */
    void lock(Jedis jedis, String lock) {

        while (true) {
            Long num = jedis.setnx(lock, jvmName);
            /**
             * 如果该线程拿到了锁，就进入if方法
             * 直接就return了，不执行后面的；
             */
            if (num == 1) {
                // 1秒后key过期->释放锁
                jedis.expire(name, 1);
                //下面这个应该更合理；
                //jedis.expire(lock,1);
                return;
            }
            /**
             * 这里需要引入之前所学的让线程不消耗资源，停在这里的方法
             * 然后当满足条件，也就是收到指定订阅的消息后，让线程继续；
             * 先计划使用countDonwLatch
             */
            CountDownLatch countDownLatch = new CountDownLatch(1);
            initSubscribe(countDownLatch);
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void initSubscribe(CountDownLatch countDownLatch) {
        final Thread thread = new Thread(() -> {
            final Jedis jedis = new Jedis();
            jedis.subscribe(new DistributedLockPubSub(countDownLatch), "DistributedLock");
        });
        thread.setDaemon(true);
        thread.start();
    }

    void unlock(Jedis jedis, String lock) {
        while (true) {
            String lockName = jedis.get(lock);
            if (lockName.equals(jvmName)) {
                jedis.del("lock");
                /**
                 * 释放锁时，发布消息
                 */
                jedis.publish("DistributedLock", "unlock");
                return;
            }
//                Thread.sleep(100);
        }
    }
}
