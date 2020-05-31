package com.github.hcsp.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class TestMain {
    public static void main(String[] args) throws Exception {
        JedisPool pool = new JedisPool();

        new DistributedLock("lock")
                .runUnderLock(
                        () -> {
                            try (Jedis jedis = pool.getResource()) {
                                String value = jedis.get("KeyUnderTest");
                                if (value == null) {
                                    value = "1";
                                } else {
                                    value = "" + (Integer.parseInt(value) + 1);
                                }
                                Thread.sleep(100);
                                jedis.set("KeyUnderTest", value);
                                return null;
                            }
                        });
    }
}
