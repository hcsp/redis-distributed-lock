package com.github.hcsp.redis;

import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.CountDownLatch;

public class DistributedLockPubSub extends JedisPubSub {

    CountDownLatch countDownLatch;

    public DistributedLockPubSub(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void onMessage(String channel, String message) {
        if (channel.equals("DistributedLock") && message.equals("unlock")) {
            countDownLatch.countDown();
        }
    }
}
