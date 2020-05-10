package cn.cqu.edu.learn.base.normaljdbc;

import cn.cqu.edu.learn.base.normaljdbc.entity.SimpleEntity;
import cn.cqu.edu.learn.base.normaljdbc.service.ISimpleService;
import cn.cqu.edu.learn.base.normaljdbc.service.impl.SimpleService;
import cn.cqu.edu.learn.base.normaljdbc.util.NormalJdbcUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.PrintWriter;
import java.sql.*;

@SpringBootTest
class NormalJdbcApplicationTests {

    @Autowired
    private ISimpleService simpleService;

    @Test
    void contextLoads() {
        try {
            System.setProperty("jdbc.drivers", "com.mysql.cj.jdbc.Driver");
            DriverManager.setLogWriter(new PrintWriter(System.out));
            Connection connection = DriverManager.getConnection("" +
                    "jdbc:mysql://localhost:3306/" +
                    "feng?" +
                    "user=root" +
                    "&password=123456" +
                    "&useUnicode=true" +
                    "&characterEncoding=utf-8" +
                    "&serverTimezone=GMT%2B8");
            String preSql = "insert into student(`id`, `name`, `age`) values(?, ?, ?)";
            Statement pre = connection.prepareStatement(preSql);
            System.err.println(connection.getHoldability());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
