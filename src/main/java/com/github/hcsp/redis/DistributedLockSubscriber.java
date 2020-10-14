package com.github.hcsp.redis;

import redis.clients.jedis.Jedis;

public class DistributedLockSubscriber {
    public DistributedLockSubscriber() {
        init();
    }

    private void init() {
        final Thread thread = new Thread(() -> {
            final Jedis jedis = new Jedis();
            jedis.subscribe(new DistributedLockPubSub(),"DistributedLock");
        });
        thread.setDaemon(true);
        thread.start();
    }
}
