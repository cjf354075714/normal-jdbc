package cn.cqu.edu.learn.base.normaljdbc;

import cn.cqu.edu.learn.base.normaljdbc.entity.SimpleEntity;
import cn.cqu.edu.learn.base.normaljdbc.entity.jdbc.SimpleDataSource;
import cn.cqu.edu.learn.base.normaljdbc.entity.jdbc.pool.SimpleConnectionPoolDataSource;
import cn.cqu.edu.learn.base.normaljdbc.entity.jdbc.pool.SimplePooledConnection;
import cn.cqu.edu.learn.base.normaljdbc.service.ISimpleService;
import cn.cqu.edu.learn.base.normaljdbc.service.impl.SimpleService;
import cn.cqu.edu.learn.base.normaljdbc.util.NormalJdbcUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.PooledConnection;
import java.io.PrintWriter;
import java.sql.*;

@SpringBootTest
class NormalJdbcApplicationTests {

    @Autowired
    private ISimpleService simpleService;

    @Autowired
    private SimpleDataSource simpleDataSource;

    @Autowired
    private SimpleConnectionPoolDataSource simpleConnectionPoolDataSource;

    @Test
    void contextLoads() {
        try {
            PooledConnection connection = simpleConnectionPoolDataSource.getPooledConnection();
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
