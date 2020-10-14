package com.github.hcsp.redis;

import redis.clients.jedis.JedisPubSub;

public class DistributedLockPubSub extends JedisPubSub implements DistributedLockQueue {
    @Override
    public void onMessage(String channel, String message) {
        if ("DistributedLock".equals(channel) && "unlock".equals(message)) {
            try {
                put("unlock");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
