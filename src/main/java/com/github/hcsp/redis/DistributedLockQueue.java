package com.github.hcsp.redis;

import java.util.concurrent.LinkedBlockingQueue;

public interface DistributedLockQueue {
    LinkedBlockingQueue<String> QUEUE = new LinkedBlockingQueue<>(1);

    default void put(String ele) throws InterruptedException {
        QUEUE.put(ele);
    }

    default String poll() {
        return QUEUE.poll();
    }
}
