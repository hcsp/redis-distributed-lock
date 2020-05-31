package com.github.hcsp.redis;

import java.util.concurrent.Callable;

public class DistributedLock {
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
        return callable.call();
    }
}
