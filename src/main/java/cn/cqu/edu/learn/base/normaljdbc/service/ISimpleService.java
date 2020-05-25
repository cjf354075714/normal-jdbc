package cn.cqu.edu.learn.base.normaljdbc.service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

public interface ISimpleService {

    void printCurrentClassLoader() throws Exception;

    void loadDriver() throws Exception;

    void driverManager() throws Exception;

    void connection(Connection connection) throws Exception;

    void resultSet(ResultSet resultSet) throws Exception;

    void dataSource(DataSource dataSource) throws Exception;
}
