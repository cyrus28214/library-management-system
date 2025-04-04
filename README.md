## 图书管理系统框架使用指南——Java

### 环境要求
- JDK 1.8.0及以上，可通过`java -version`命令查看
- Apache Maven 3.6.3及以上，可通过`mvn -v`命令查看

`resources`目录下存放了数据库连接的相关配置以及Sql脚本

清理输出目录并编译项目主代码
`mvn clean compile`

运行主代码
`mvn exec:java -Dexec.mainClass="Main" -Dexec.cleanupDaemonThreads=false`

运行所有的测试
`mvn -Dtest=LibraryTest clean test`

运行某个特定的测试
`mvn -Dtest=LibraryTest#parallelBorrowBookTest clean test`

### 文档

[ApiFox文档](https://w49pxtebrt.apifox.cn)

### 注

由于我实现了修改借书卡信息的功能，我在`LibraryTest.java`中添加了新的单元测试`modifyCardInfoTest`，其他测试方法均未改动。