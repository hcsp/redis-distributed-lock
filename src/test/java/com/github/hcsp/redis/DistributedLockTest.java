package com.github.hcsp.redis;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

class DistributedLockTest {
    /** A distributed test which starts 10 JVMs. */
    @Test
    public void distributedTest() throws Exception {
        List<Process> processes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String javaPath = System.getProperty("java.home") + "/bin/java";
            String classPath = System.getProperty("java.class.path");

            ProcessBuilder pb =
                    new ProcessBuilder(
                            javaPath, "-cp", classPath, "-DjvmName=jvm" + i, "com.github.hcsp.redis.TestMain");
            processes.add(pb.start());
        }

        for (Process process : processes) {
            Assertions.assertEquals(0, process.waitFor());
        }

        JedisPool pool = new JedisPool();
        try (Jedis jedis = pool.getResource()) {
            Assertions.assertEquals("10", jedis.get("KeyUnderTest"));
        }
    }

    @AfterEach
    public void cleanUp() {
        JedisPool pool = new JedisPool();
        try (Jedis jedis = pool.getResource()) {
            jedis.del("KeyUnderTest");
        }
    }
}
