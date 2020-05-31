# Redis实战：实现一个分布式锁

在课上，我们利用Redis编写了一个简单的分布式锁。但是它还有很多的不足：

- 使用`Thread.sleep()`代替通知机制。这种方式比较浪费资源，应该使用其他的方式（比如Redis的发布/订阅，因为上锁的线程可能处于不同JVM中）；
- 没有Timeout机制。

请实现一个分布式锁，尽可能地解决上述问题。

测试需要首先启动一个监听`localhost:6379`的Redis实例。

祝你好运！！

在提交Pull Request之前，你应当在本地确保所有代码已经编译通过，并且通过了测试(`mvn clean verify`)

-----
注意！我们只允许你修改以下文件，对其他文件的修改会被拒绝：
- [src/main/java/com/github/hcsp/redis/](https://github.com/hcsp/redis-distributed-lock/blob/master/src/main/java/com/github/hcsp/redis/)
- [pom.xml](https://github.com/hcsp/redis-distributed-lock/blob/master/pom.xml)
-----

