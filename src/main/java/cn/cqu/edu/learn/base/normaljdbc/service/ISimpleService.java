package cn.cqu.edu.learn.base.normaljdbc.service;

import java.sql.Connection;

public interface ISimpleService {

    void printCurrentClassLoader() throws Exception;

    void loadDriver() throws Exception;

    void driverManager() throws Exception;

    void connection(Connection connection) throws Exception;
}
