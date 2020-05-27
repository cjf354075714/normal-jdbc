package cn.cqu.edu.learn.base.normaljdbc.entity.jdbc;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.*;
import java.util.logging.Logger;

public class SimpleDataSource implements DataSource {

    static {
        System.setProperty("jdbc.drivers", "com.mysql.cj.jdbc.Driver");
        DriverManager.setLogWriter(new PrintWriter(System.out));
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("");
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
