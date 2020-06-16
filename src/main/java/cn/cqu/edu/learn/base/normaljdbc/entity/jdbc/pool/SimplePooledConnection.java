package cn.cqu.edu.learn.base.normaljdbc.entity.jdbc.pool;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;
import javax.sql.StatementEventListener;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public class SimplePooledConnection implements PooledConnection {

    private final List<ConnectionEventListener> connectionEventListenerList = new LinkedList<>();

    private Connection physicalConn;

    public Connection getPhysicalConn() {
        return physicalConn;
    }

    public void setPhysicalConn(Connection physicalConn) {
        this.physicalConn = physicalConn;
    }

    public SimplePooledConnection(Connection connection, ConnectionEventListener listener) {
        this.physicalConn = connection;
        this.addConnectionEventListener(listener);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return null;
    }

    @Override
    public void close() throws SQLException {
        ConnectionEvent connectionEvent = new ConnectionEvent(this);
        connectionEventListenerList.forEach(
                index -> index.connectionClosed(connectionEvent)
        );
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        connectionEventListenerList.add(listener);
    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        connectionEventListenerList.remove(listener);
    }

    @Override
    public void addStatementEventListener(StatementEventListener listener) {

    }

    @Override
    public void removeStatementEventListener(StatementEventListener listener) {

    }
}
