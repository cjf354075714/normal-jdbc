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
     */

    @Override
    public void connection(Connection connection) throws Exception {
        System.err.println(connection.getAutoCommit());
    }
}
