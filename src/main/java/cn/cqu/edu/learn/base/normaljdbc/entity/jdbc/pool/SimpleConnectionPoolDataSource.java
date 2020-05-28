package cn.cqu.edu.learn.base.normaljdbc.entity.jdbc.pool;

import org.springframework.stereotype.Component;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;
import java.io.PrintWriter;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component("simpleConnectionPoolDataSource")
public class SimpleConnectionPoolDataSource implements ConnectionPoolDataSource, ConnectionEventListener {

    private static final Logger utilLog;

    static {
        utilLog = Logger.getLogger(SimplePooledConnection.class.getName());
    }

    private Stack<SimplePooledConnection> pool;

    public Stack<SimplePooledConnection> getPool() {
        return pool;
    }

    public void setPool(Stack<SimplePooledConnection> pool) {
        this.pool = pool;
    }

    public SimpleConnectionPoolDataSource() {
        System.setProperty("jdbc.drivers", "com.mysql.cj.jdbc.Driver");
        DriverManager.setLogWriter(new PrintWriter(System.out));

        pool = new Stack<>();

        for (int i = 0; i < 5; i++) {
            try {
                pool.push(new SimplePooledConnection(DriverManager.getConnection("jdbc:mysql://localhost:3306/" +
                "feng?" +
                        "user=root" +
                        "&password=123456" +
                        "&useUnicode=true" +
                        "&characterEncoding=utf-8" +
                        "&serverTimezone=GMT%2B8"), this));
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }

    @Override
    public PooledConnection getPooledConnection() throws SQLException {
        if (pool.isEmpty()) {
            return null;
        } else {
            return pool.pop();
        }
    }

    @Override
    public PooledConnection getPooledConnection(String user, String password) throws SQLException {
        return null;
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

    @Override
    public void connectionClosed(ConnectionEvent event) {
        Object o = event.getSource();
        SimplePooledConnection connection = null;
        if ( o instanceof SimplePooledConnection ) {
            connection = (SimplePooledConnection) o;
            try {
                utilLog.log(Level.INFO, connection.getPhysicalConn().getClientInfo().toString());
                pool.add(connection);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        utilLog.log(Level.INFO, "链接关闭");
    }

    @Override
    public void connectionErrorOccurred(ConnectionEvent event) {

    }
}
