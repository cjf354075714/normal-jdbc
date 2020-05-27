package cn.cqu.edu.learn.base.normaljdbc.service.impl;

import cn.cqu.edu.learn.base.normaljdbc.service.ISimpleService;
import org.springframework.stereotype.Service;

import javax.sql.CommonDataSource;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;


/**
 * 待看的知识点
 * ResultSet 的游标、可见性、abort方法
 */
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
     * 链接：找到了这个二进制文件，接着虚拟机就去加载这个二进制文件。加载的结果就是，后面的操作就可以立马执行了，
     * 然后就是链接内部又分为：验证、准备、解析
     * 初始化：执行静态代码块和静态变量的赋值，谁执行？静态常量，但我们可以在静态块中去给这些变量赋值，
     * 但是内部常量是没有赋值的，内部方法不管是静态与否都不执行
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
     *
     * 学习1：多次学习加深印象
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
     *
     * 学习2：
     * 什么是事务？
     * 是数据库自己规定的一种 SQL 执行单位
     * 无论是不是多条 SQL 语句，都是一个事务，自然具有事务自己的特性
     * 我们通常执行 SQL 的时候，都是一条一条地执行，数据库会默认以事务的方式去执行这些 SQL
     *
     * 事务的特性：
     * 原子性：一条、两条、多条数据，是一个执行的单位，不可分割，没啥可讨论的
     * 一致性：一个事务执行的时候，一定是执行完毕，或者执行失败，不会失败一部分或者成功一部分，和原子性结合在一起
     * 持久性：事务执行完毕之后，对数据库产生的影响一定是永久的，没啥可说的
     * 隔离性：最重要，它表示自己这个事务，对其他事务所产生结果的访问权限，或者说是能够访问到哪些数据
     * 四个性质，数据库有自己的实现方式，我们暂时不去讨论实现原理，先去考虑，这些性质
     * 对我们开发而言，有什么影响，或者说，我们开发，有什么权限去影响，去操作这些性质
     *
     * 原子性、一致性、持久性都是数据库自己实现的，我们不管，也管不了
     *
     * 隔离性：
     * 口水话：两个事务，它们隔得有多远（新冠疫情，有多远隔多远），四个等级
     * 读未提交：1
     * 读已提交：2
     * 可重复读：4
     * 串行化：8
     * 对于一个事务而言，它有自己的隔离性，如果没有主动设置，默认就是数据的系统隔离级别
     * 大部分数据库隔离级别是读已提交，MySQL 是可重复读
     * 隔离级别的高低对数据库 CRUD 有什么影响吗？
     *
     * A 事务的隔离级别为读未提交，则 A 事务可以读取到任何事务的没有提交的数据（其他事务的隔离级别，没有用）
     * A 事务的隔离级别为读已提交，则 A 事务可以读取到已提交的数据，也只能读取到已提交的数据
     * A 事务的隔离级别为可重复读，则 A 事务可以在一个事务内，多次读取，且读取到的数据是相同的
     * A 事务的隔离级别为串行化，那 A 事务提交之前，其他所有事务，都不能访问该事务所锁定的信息？锁定的什么？
     *
     * 隔离级别能解决什么问题？这些问题有实际的应用吗？
     * 读未提交导致什么问题就不解决，一般不用。（脏读）
     *
     * 读已提交可以解决事务读取脏数据的问题，比如 A 事务的隔离级别是读已提交，那它只能读取已经提交的数据
     * 但是没有解决：一个事务读取两次数据，数据会不一样的问题，什么情况下，会在一个事务中去查询两次数据呢？刷卡
     *
     * 可重复读可以解决单个事务内，读取的数据一定是相同的问题。但是它就只能读取到当前时间点之前的数据，也就是说
     * 你无论怎么读，也无法读取到其他事务写入的数据，也就是幻读问题
     *
     * 串行化就是所有事物全部串行执行，具体到数据库上，MySQL 需要两个串行化两个事物，如果其中一个事务去
     * 修改数据，则另外的事务就得等着。串行化，解决了所有的问题
     *
     *
     * 这些隔离级别，他是如何实现的？用什么锁？
     * 悲观锁、乐观锁、共享锁、排它锁
     * 重点：明天看
     *
     *
     * // 获取 Connection 的时候，数据库返回的异常
     * SQLWarning getWarnings() throws SQLException;
     *
     * // 设置该对象得到的 ResultSet 的可见性等级，可见性等级有什么用？
     * void setHoldability(int holdability) throws SQLException;
     *
     * // 获取可见性
     * int getHoldability() throws SQLException;
     *
     *
     * // 设置一个 SavePoint，SavePoint 这个概念是数据库中的
     * 本质上是一个事务运行过程中，它的一个状态，比如：
     * 开启事务：
     * insert 1
     * insert 2
     * save point a
     * delete 3
     * save point b
     * roll back savepoint a
     * 则数据库就回滚到 insert 2 之后的状态，这个比较简单
     * 但是，我们在 mybatis spring 中如何去设置呢？
     * rollback 可以直接指定返回到那哪个 SavePoint
     * 还有什么，release 等等
     * Savepoint setSavepoint() throws SQLException;
     *
     * createClob createBlob createNClob 等方法，本质上都是去设置数据库字段类型的
     * 我们不需要关心这些，因为现在数据库都不存储大数据了，都是存链接，知道有这个概念就行
     *
     *
     * 创建一个 SQLXML 对象，因为现在数据里面可以存 XML 了
     * 这个我们也不需要去看了，知道就行
     * SQLXML createSQLXML();
     *
     *
     * // 在 timeout 秒内，返回该链接是否可用，记住这个方法
     * boolean isValid(int timeout);
     *
     * setClientInfo、getClientInfo 用来设置客户端信息，记住就行了
     *
     * Array createArrayOf() Struct createStruct 两个方法都是为数据库
     * 提供的特殊结构的方法，知道就行了
     *
     * setSchema() getSchema() 设置获取数据库中的 Schema
     * Schema 是数据库中的概念，比如，表结构，视图等其他结构的定义 String
     *
     * // 我明白倒是明白，但是我不知道这个 executor，哪来啊？
     * void abort(Executor executor) 终止一个链接对象
     *
     * setNetworkTimeout、getNetworkTimeout 设置链接超时时间，知道就行
     *
     * 至此，Connection 学习完了
     */

    @Override
    public void connection(Connection connection) throws Exception {
        System.err.println(connection.getTypeMap());
    }

    /**
     * 一个能够被重复使用的物理数据库链接对象
     * 当应用调用 DataSource.getConnection 的时候，如果 ConnectionPoolDataSource
     * 已经准备好了，则驱动会返回 Connection 对象，然后将这个对象，转化成 PooledConnection
     *
     * 当调用 Connection.close 方法的时候，链接管理池就会被通知到，因为，管理池对象已经把自己
     * 注册成一个监听器，通过 addConnectionEventListener 方法，就是说，关闭的时候，管理者回去
     * 自动将该对象，返回到连接池中，而不是自动关闭
     * @param pooledConnection pooledConnection
     * @throws Exception Exception
     */
    @Override
    public void pooledConnection(PooledConnection pooledConnection) throws Exception {

    }

    /**
     * ResultSet 这个对象，代表着数据库中表的数据，通常是执行 Statement
     * 来返回这个对象
     * ResultSet 在初始化之后，会自带一个游标指针，该指针初始化指向第一行数据的前面
     * ResultSet 还有一个 next 方法，会将游标下一一行，然后，返回是否移动成功
     * 如果是最后一行，则返回 false，我们可以使用这个方法，在 while 中进行迭代
     * 默认的 ResultSet 对象是不可更新的，且游标只能向下移动，只能从第一行移动到最后一行
     * 当然也可以创建可滚动的和可更新的 ResultSet 对象。
     * @param resultSet resultSet
     * @throws Exception Exception
     *
     *
     * 知识点1：
     * 游标的上下滚动
     * 获取数据的可更新性
     *
     * 首先，游标是可以上下滚动的，默认是从上往下滚动，但是，在滚动的过程中，可以设定
     * 是否去关注，其他人更新了某一行数据，比如，我去更新了第 x 行数据，他刚好就移动
     * 到 x 行，TYPE_SCROLL_INSENSITIVE 就表示，不需要去获取这个更新，游标有三个等级
     *
     * TYPE_FORWARD_ONLY 游标只能从上往下移动
     * TYPE_SCROLL_INSENSITIVE 游标任意滚动，但是对数据的更改不敏感
     * TYPE_SCROLL_SENSITIVE 游标任意滚动，且关注数据的更改
     *
     * ResultSet 接口，提供很多 get 方法，用于获取每一行的数据，建议使用行索引的方式
     * 他们的效率更高。列索引从 1 开始一直到列数的最大值，每一列应该被阅读于一次，且顺序是从左到右
     *
     * ResultSet 自己负责数据库和 JAVA 类型的映射
     *
     * JDBC 2.0 在 ResultSet 中提供了更新列函数，和新增列函数
     * 当然，是更新到数据库中的，且游标的类型应该是上下移动，对数据更改敏感
     *
     * 更新列：
     *
     *  rs.absolute(5); // moves the cursor to the fifth row of rs
     *  rs.updateString("NAME", "AINSWORTH"); // updates the <code>NAME</code> column of row 5 to be <code>AINSWORTH</code>
     *  rs.updateRow(); // updates the row in the data source
     *
     * 新增列：新增列有点不同的是，它需要先移动到插入到的行，然后去更新这一行，最后，去插入
     *
     * rs.moveToInsertRow(); // moves cursor to the insert row
     * rs.updateString(1, "AINSWORTH"); // updates the first column of the insert row to be <code>AINSWORTH</code>
     * rs.updateInt(2,35); // updates the second column to be <code>35</code>
     * rs.updateBoolean(3, true); // updates the third column to <code>true</code>
     * rs.insertRow();
     * rs.moveToCurrentRow();
     *
     * 没了，这个对象没啥值钱的了
     *
     */
    @Override
    public void resultSet(ResultSet resultSet) throws Exception {

    }

    /**
     * CommonDataSource 是 JAVA DataSource 这个概念中的顶级父类
     * 就是说，有很多 DataSource 他们有共同的父接口，这个接口里面就是去设置打印流的
     * 所以，你看啊，以前的 JAVA 还没有日志流，只能用输出流了
     * @param commonDataSource commonDataSource
     * @throws Exception Exception
     */
    @Override
    public void commonDataSource(CommonDataSource commonDataSource) throws Exception {

    }

    /**
     * 这个对象，是 PooledConnection 的工厂类
     * 这个数据库链接对象，有些不一样，是可以重用的，具体应该看 PooledConnection 这个类
     * @param connectionPoolDataSource connectionPoolDataSource
     * @throws Exception Exception
     */
    @Override
    public void connectionPoolDataSource(ConnectionPoolDataSource connectionPoolDataSource) throws Exception {

    }

    /**
     * DataSource 对象，是 SQL 协议的标准接口，主要是只是来获取 Connection 对象
     * 具体的数据库厂商去实现这个接口，有三种比奥准的实现
     *
     * 1, 简单的 Connection 对象获取，它和 DriverManager 的 Connection 获取是一致的
     * 2，典型的资源池管理方式，获取多个 Connection，存在内存中的，用完之后直接返回，并不关闭（怎么返回呢？）
     * 3，分布式事务的 Connection 获取，一个 Connection 可以访问多个数据库
     * @param dataSource dataSource
     * @throws Exception Exception
     */
    @Override
    public void dataSource(DataSource dataSource) throws Exception {

    }
}
