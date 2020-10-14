package com.github.hcsp.redis;

import java.util.concurrent.LinkedBlockingQueue;

public interface DistributedLockQueue {
    LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>(1);

    default void put(String ele) throws InterruptedException {
        queue.put(ele);
    }

    default String poll(){
        return queue.poll();
    }
}
