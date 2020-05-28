package cn.cqu.edu.learn.base.normaljdbc.entity.jdbc;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;


/**
 * 这就是一个简单的 DataSource 类，但是没有实现链接可回收
 */
@Component("simpleDataSource")
public class SimpleDataSource implements DataSource {

    static {
        System.setProperty("jdbc.drivers", "com.mysql.cj.jdbc.Driver");
        DriverManager.setLogWriter(new PrintWriter(System.out));
    }

    public SimpleDataSource() {

    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/" +
        "feng?" +
                "user=root" +
                "&password=123456" +
                "&useUnicode=true" +
                "&characterEncoding=utf-8" +
                "&serverTimezone=GMT%2B8");
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return null;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {

    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {

    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }
}
