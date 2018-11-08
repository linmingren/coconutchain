# coconutchain
一个用来学习的区块链实现

# 编译

```
mvn clean compile assembly:single
```
上面的命令会把所有依赖库都打包在一个jar中

# 运行

```
java -jar target/coconut-chain-1.0-SNAPSHOT-jar-with-dependencies.jar
```

# 使用的第三方库

* [Spark](https://github.com/perwendel/spark) 一款轻量级的web框架，用来对外提供REST接口
* [Bouncy Castle](http://bouncycastle.org/java.html) 使用它来生成钱包的公私钥
* [FasterXML Jackson](https://github.com/FasterXML/jackson) 用来进行POJO和JSON之间的转换
* [lombok](https://projectlombok.org/) 在POJO类加上注解，从而免去手写getter/setter等常见函数
* [Slf4j](http://www.slf4j.org/) 记录日志
* [Netty](https://www.netty.io/) p2p通信

