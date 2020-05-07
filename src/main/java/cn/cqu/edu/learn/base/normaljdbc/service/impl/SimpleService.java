package cn.cqu.edu.learn.base.normaljdbc.service.impl;

import cn.cqu.edu.learn.base.normaljdbc.service.ISimpleService;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;

@Service("simpleService")
public class SimpleService implements ISimpleService {


    /**
     * 当前的类加载器，类加载器分为三类，这里是最低等级的应用加载器
     *
     * 其他等级的加载器先不看
     * @throws Exception 异常
     */
    @Override
    public void printCurrentClassLoader() throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        System.err.println(classLoader.toString());
    }

    /**
     * Class.forName 到底是什么方法
     * 首先，虚拟机去获取、执行一个类的步骤有哪几步？
     *
     * 加载->链接->初始化->使用->卸载
     *
     * 加载：通过一个类的全限定名去找到该类的二进制文件，这里的全限定名说的是 package.name + class.name
     * 链接：找到了这个二进制文件，接着虚拟机就去加载这个二进制文件。加载的结果就是，后面的操作就可以立马执行了，然后就是链接内部又分为：验证、解析等等
     * 初始化：执行静态代码块和静态变量的复制，但是内部常量是没有赋值的，内部方法不管是静态与否都不执行
     * 废话，非静态的变量和方法，你不去生成类实例，你根本都没有调用的途径，自然不会初始化
     * 使用和卸载不说了，但是使用和初始化之间，有一个过程，就是 new 或者 newInstance，这个过程将初始化这些变量（内部变量）
     * 这部分后面将它改到 base 上面去
     *
     * 那么，我们有哪些方法方式去主动出发加载、链接、初始化、使用、卸载呢？
     * Class.forName("class path") 会执行静态代码块，初始化静态变量
     * ClassLoader.loadClass("class path") 不会执行静态代码块，不会初始化静态变量
     * class class = new class() 执行所有代码块，初始化所有变量
     * 主动执行静态代码块，初始化静态变量
     * 再强调一遍：方法不管是不是静态，都不会主动执行
     * 静态代码块只会执行一次，静态变量也只会初始化一次
     *
     * @throws Exception 异常
     */
    @Override
    public void loadDriver() throws Exception {
        // 这行代码，本质上就是去执行 Driver 的静态代码块和给 Driver 这个静态变量初始化值
        Class.forName("com.mysql.cj.jdbc.Driver");
    }

    /**
     * DriverManager 类的详解
     * @see java.sql.DriverManager
     * 这个类是数据库驱动注册，链接获取的主要类
     * @throws Exception Exception
     *
     * DriverManager 是一个管理一系列 JDBC 驱动的基础服务。
     * 在 JDBC 2.0 API 中，提供了另外一个接口去链接数据库
     * @see javax.sql.DataSource
     * 这也是更好的链接数据库的方式
     *
     * 作为自己初始化的一部分，DriverManager 会企图加载系统配置中被引用为 jdbc.drivers 的驱动
     * 类。这种方式就允许我们自己去自定义自己的 JDBC 驱动。采用这种方式，需要给系统设置参数值
     *
     * ``` java
     * // 冒号隔开
     * System.setProperty("java.drivers", "com.mysql.cj.jdbc.Driver:foo.bah.Driver");
     * ```
     * DriverManager 有两个方法：getConnection、getDrivers。这两个访问已经被增强了，用于适配
     * 标准版 JAVA 服务提供机制。JDBC 4.0 的驱动必须包含一个目录为
     * <code>META-INF/services/java.sql.Driver</code>。
     * 的文件，这文件存在于 JAR 包中的根目录，里面的内容就是实现了
     * @see java.sql.Driver
     * 接口的类的全局限定名
     *
     * 应用不在需要明确地使用 Class.forName("driver Path") 加载类驱动，之前的应用不需要修改就可以
     * 继续使用
     *
     * 当我们主动去调用 getConnection 的时候，DriverManager 会去已经初始化好了的驱动中
     * 找到和当前应用同一个类加载器加载的驱动，然后去获取链接。因为在调用这个静态方法的时候，DriverManager
     * 的静态代码块，已经被执行了，这个代码块，就是去加载驱动的，本质上还是去获取 System.getProperty();
     * 然后，再使用 Class.forName("driver path");
     *
     * DriverManager 是否有一个严重的 BUG？
     * DriverManager 有一个静态代码块，这个代码块就是去初始化数据库驱动的。但是这个方法也会调用日志输出
     * 得，你日志输入类没有初始化，自然就不会输出，可是你去设置日志输出得时候，就一定会先去调用静态代码块
     * 所以，静待代码块的日志输出是不是没有意义？是不是我还有什么地方没有接触到
     *
     * 以后我怎么去加载数据库驱动：
     * System.setProperty("jdbc.drivers", "com.mysql.cj.jdbc.Driver");
     * DriverManager.setLogWriter(new PrintWriter(System.err));
     * DriverManager.getConnection("jdbc url");
     *
     *
     *
     * 核心代码分析：
     *
     * static {
     *     loadInitialDrivers();
     * }
     *
     * loadInitialDrivers 方法：
     * * 获取系统配置 System.getProperty("driverPath1:driverPath2:...");
     * * forEach -> ( Class.forName("driverPath1") )
     * 就是去执行配置中的 Driver 的 static 代码块
     *
     * class Driver
     *     static {
     *         try {
     *             DriverManager.registerDriver(new Driver());
     *         } catch (SQLException var1) {
     *             throw new RuntimeException("Can't register driver!");
     *         }
     *     }
     *
     * // 注册驱动本质上就是给这个 DriverManager 中的线程安全集合类添加一个 Dirver 对象
     * void registerDriver (driver) {
     *     // registeredDrivers 是一个线程安全的集合类
     *     // DriverInfo 是 driver 的一个包装类
     *     registeredDrivers.addIfAbsent(new DriverInfo(driver, da));
     * }
     *
     * // 通过 URL 获取一个数据库链接
     * // 本质上是调用的 Driver 的 connect 函数，这个函数的实现是驱动提供者自己实现的
     * // 这个方法有很多个实现，本质上都是传入不同的参数，以后就用这个函数去获取链接
     * // 就是将数据库的链接信息全部写入到 URL 中
     * Connection getConnection(String url) throws SQLException {
     *
     * }
     *
     * // 通过 url 获取能够识别该 url 的驱动
     * // 比如传入 mysql 协议的 url，就应该返回 mysql 的驱动
     * Driver getDriver (String url) throws SQLException {
     *
     * }
     *
     * 其他的方法就是日志类的相关处理，和驱动是否可用等
     * 这个类就学习完毕了
     *
     *
     */
    @Override
    public void driverManager() throws Exception {

    }

    /**
     *
     * @param connection connection
     * @throws Exception Exception
     * 该对象代表着一个具体的数据库链接对象，SQL 的执行和结果的返回都是这个对象的上下文中执行的
     * @see java.sql.Connection
     * 数据库的详细信息比如：表、支持的 SQL 编程、存储的存储过程，链接的能力等都能够通过该对象访问
     * getMetaData() 将返回这些数据。
     *
     *
     * 当配置 Connection 对象的时候，JDBC 应用应该使用合适的方法，比如：
     * setAutoCommit 或者是 setTransactionIsolation。应用不应该使用 SQL 语言的方式
     * 去直接更改 Connection 的配置。默认情况下，Connection 对象会在会话执行之后自动提交
     * 如果不是自动提交模式，则需要去显示的提交数据。否则数据不会被保存
     *
     * 使用 JDBC 核心 API 获取的 Connection 对象，会带着一个初始化的空的联带 Map 对象。
     * 程序员可以使用自己的映射 UDT，放在这个 Map 中
     * UDT 什么是 UDT，我自己理解，就是数据库表字段类型和 JAVA 数据类型的映射关系
     * 当我们执行 SQL 查询之后，ResultSet.getObject 去获取数据时，会先判断有没有我们自定义的数据映射关系
     * 有的话，就使用，没有的话，就使用标准的映射关系，这里就需要我们去判断了
     *
     * 比如，我 SQL 查询一个表，这个表就是一个 JAVA 实体。如果直接查询，使用这种方法，是不是就直接返回实体呢？
     *
     * Map<String, Class<?>> map = connect.getTypeMap();
     * map.put("myEntityName", Class.forName("myEntityName.path"));
     * map.setTypeMap(map);
     *
     * 核心方法：
     *
     *
     * 创建并返回一个可以执行 SQL 的 Statement 对象
     * 没有 SQL 参数的执行请求，一般都是使用 Statement 对象
     * 如果相同的 SQL 会被执行多次，使用 PreparedStatement 对象也许会有性能上的提升
     * Statement createStatement() throws SQLException;
     *
     * Statement 是使用默认的类型 TYPE_FORWARD_ONLY 和默认的并发等级 CONCUR_READ_ONLY
     * 去返回一个 ResultSet
     *
     * 这两个参数，放在 ResultSet 上去看
     *
     * ResultSet 的可保存性，可以通过 getHoldability 方法返回
     * Statement createStatement() throws SQLException;
     *
     *
     *
     * 创建一个 PreparedStatement 对象，没有参数和有参数的 SQL 能够被预编译和存储到该对象中
     * 这个对象能够被多次使用，这里的多次使用我没看懂
     * 有些驱动不支持 SQL 的预编译，如果支持预编译，则驱动会将 SQL 发送到数据库去执行预编译。
     * 如果不支持，就不会发送，这个区别对于开发人员来说是没有的，但是会抛出异常。
     * 同步级别和游标行为和 createStatement 相同
     * 这里需要传递一个 String 参数。就是 SQL 的字符串，在里面，有很多 ‘？’，用于字符串的替换符
     *
     * PreparedStatement prepareStatement(String sql)
     *         throws SQLException;
     *
     *
     * 存储过程涉及到的创建方法，和预编译一样，都是将 SQL 发送到数据库，然后执行
     * 存储过程并不直接在参数中去写，只是我们在数据库中写好了之后，再去调用
     * 这里我先不看了
     * CallableStatement prepareCall(String sql) throws SQLException;
     *
     *
     * 该方法就是 SQL 的翻译，比如将 Oracle 翻译成 Mysql，
     * 但是我没有测试过，也许是可用
     * 那么，将方言翻译成那个版本的 SQL 呢，应该是我这个 Connection 是怎么来的
     * 就是来自哪个驱动，就翻译成哪个驱动的方言
     *
     * String nativeSQL(String sql) throws SQLException;
     *
     *
     * 设置事物是否自动提交，本质上 JDBC 去执行 SQL 都是以事物的方式
     * DML DDL 在执行完毕之后，会在不同的时间点上，去提交事物
     * 默认获取的 Connection 对象都是自动提交的
     * 以后我要是获取链接，主动去设置好，不自动提交事物
     * void setAutoCommit(boolean autoCommit) throws SQLException;
     *
     * 获取是否自动提交
     * boolean getAutoCommit() throws SQLException;
     *
     *
     * 提交上一次的所有更改和回滚，我这里不明白，啥叫提交回滚，是不是我回滚之后
     * 还要去提交一次呢？提交完毕之后，会释放该链接对于该数据库的锁
     * 1，这是什么锁？行锁、表锁还是什么
     * 2，什么时候去获取的锁呢？
     * void commit() throws SQLException;
     *
     *
     * 回滚上次的所有更改，并释放所有锁
     * void rollback() throws SQLException;
     *
     * 释放该对象所拥有的 JDBC 和数据库的所有资源，立马释放，而不是等待事物提交完毕之后
     * 所以，在异常中，就需要去释放，也就是 close
     * 现在，这些对象，基本都实现了 AutoCloseable 接口，在虚拟机回收的时候
     * 发现实现了该接口，那就自动去调用 close()
     * void close() throws SQLException;
     *
     *
     * 返回，该对象是否被关闭了，具体在什么时候调用，没时间看了
     * boolean isClosed() throws SQLException;
     *
     * 检索一个 DatabaseMetaData 对象，这个对象就是该数据库链接的元数据信息、
     * 比如，有哪些 table 啊，所支持的 SQL 编译器啊，存储过程等
     * DatabaseMetaData getMetaData() throws SQLException;
     *
     * 设置该连接对象只可读，不可写
     * void setReadOnly(boolean readOnly) throws SQLException;
     * boolean isReadOnly() throws SQLException;
     *
     * 设置当前数据库的访问链接的数据库名
     * 比如，我这个数据库链接有五个数据库，我通过这个方法就可以去设置
     * 这个似乎就达到了，切换数据库链接的目的，现在还没有测试，那就先留着
     * void setCatalog(String catalog) throws SQLException;
     * String getCatalog() throws SQLException;
     *
     *
     *
     * 接下来是，数据库隔离级别的学习
     * 什么是事务：
     * 多个数据库操作的一个集合，加上某些特征，就叫一个事务
     * 数据库执行事务的时候，有四个特点；ACID
     * Atomicity：原子性，多个操作不可分割，要么同时成功，要么同时失败
     * Consistency：一致性，就是数据库中的数据，业务数据的变化，要符合逻辑，这是程序员去控制的
     * Isolation：隔离性，事务之间的执行相互不影响
     * Durability：持久性，事务执行完毕之后，就对数据库产生了永久的影响
     *
     * 如何保证事务的隔离性？
     * 常见的数据库事务之间的隔离问题：
     * 这里用线程切换来表示多个事务的执行顺序
     *
     * 1，脏写：
     * A 事务读取数据，
     * 线程切换，
     * B 事务读取相同数据，写入，提交。
     * 线程切换
     * A 事务写入刚才读取的数据，然后回滚提交
     * 数据库中就写入了最开始之前的数据，但应该是 B 事务 提交的数据
     * 这里就是没有考虑到 D 这个特性，也就是数据库事务执行完毕之后，对数据库的影响是永久的
     * 当然还有隔离性也没考虑到
     *
     * 2，更新丢失：
     * A 事务读取数据
     * 线程切换
     * B 事务读取数据
     * B 事务将读取的数据进行更改，提交
     * 线程切换
     * A 事务将之前读取的数据，进行更改，并提交
     * 数据库中的数据，本来是 A B 事务一起用下的数据，但是由于 A 读取的数据
     * 是过期的，所以就感觉上是 B 事务没起作用一样，这就是更新丢失
     * 脏写跟更新丢失有个锤子区别，我佛了，我就说哪来的脏写
     * 对不起，真有区别，
     *
     * 3，脏读：
     * A 事务读取数据，写入更改
     * 线程切换
     * B 事务读取 A 事务写入的更改
     * 线程切换
     * A 事务回滚
     * 事务切换
     * B 事务读取到的就是没有提交的数据，那就是脏数据
     * 和丢失更新差不多，就是在人家事务没有完成的时候，你就去读取了
     * 没有保证隔离性
     *
     * 4，不可重复读
     * A 事务读取数据
     * 线程切换
     * B 事务读取数据，更改，提交
     * 线程切换
     * A 再次读取数据，发现两次读取数据不一样
     * 不可重复读，之前我一直搞不懂是什么意思？他和我们之前的普通
     * 读取-更改-读取，有什么区别吗
     * 有，人家的读取，是一个完整的事务，你这里，没有保证 A 事务完成之后
     * 再去执行 B 事务，所有，不可重复读
     *
     * 5，幻读
     * A 事务执行批量查询或者批量更新
     * 线程切换
     * B 事务去更新或者删除，之前 A 事务操作过的数据
     * 线程切换
     * A 事务再去读取，发现两次结果不一样，好像，我刚才操作没有全面？
     * 幻读和不可重复度的区别在于，“批量、更新、删除”
     * 还是因为 A 事务没有执行完毕，就切换了 B 事务去执行
     *
     * 现在，我们已经分析好了，事务，事务之间犹豫隔离性的问题
     * 会对一致性产品一些问题，说白了，就是，两个事务搁不开，
     * 然后，对同一个表中的同一行，或者多行数据，产生了错误的影响
     *
     * 接着分析，如何来取决这个一致性和隔离性的取舍问题
     * 自然，隔离得越远；那肯定数据就越安全，一致性就一定越高
     * 隔离的等级有四个：
     * 读未提交：READ_UNCOMMITED 什么都没解决
     * 读已提交：READ_COMMITED 解决了脏读
     * 可重复读：REPEATABLE_READ 解决了不可重复读
     * 串行化：SERIALIZABLE 解决了任何形势的数据不安全问题，解决了幻读
     *
     * 这些隔离级别，他是如何实现的？用什么锁？
     * 悲观锁、乐观锁、共享锁、排它锁
     *
     */

    @Override
    public void connection(Connection connection) throws Exception {
        System.err.println(connection.getTypeMap());
    }
}
