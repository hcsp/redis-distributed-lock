package com.github.hcsp.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Callable;

public class DistributedLock {
    //Redis 成功返回结果标识
    private static final String LOCK_SUCCESS = "OK";
    //Reis 操作返回成功
    private static final Long RELEASE_SUCCESS = 1L;
    /**
     * The lock name. A lock with same name might be shared in multiple JVMs.
     */
    private String name;
    private JedisPool pool;

    public DistributedLock(String name) {
        this.name = name;
        pool = new JedisPool();
    }

    /**
     * Run a given action under lock.
     *
     * @param callable the action to be executed
     * @param <T>      return type
     * @return the result
     */
    public <T> T runUnderLock(Callable<T> callable) throws Exception {
        T call = null;
        String value = UUID.randomUUID().toString();
        if (lock(name, value)) {
            call = callable.call();
            unLock(name, value);
        }
        pool.close();
        return call;
    }

    boolean lock(String name, String id) {
        try (Jedis jedis = pool.getResource()) {
            while (true) {
                String result = jedis.set(name, id, SetParams.setParams().nx().ex(10));
                if (LOCK_SUCCESS.equals(result)) {
                    return true;
                }
            }
        }
    }

    void unLock(String name, String id) {
        try (Jedis jedis = pool.getResource()) {
            while (true) {
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                Object result = jedis.eval(script, Collections.singletonList(name), Collections.singletonList(id));
                if (RELEASE_SUCCESS.equals(result)) {
                    break;
                }
            }
        }

    }
}
