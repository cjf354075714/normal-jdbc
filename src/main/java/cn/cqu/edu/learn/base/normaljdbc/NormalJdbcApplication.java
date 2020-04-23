package cn.cqu.edu.learn.base.normaljdbc;

import cn.cqu.edu.learn.base.normaljdbc.service.ISimpleService;
import cn.cqu.edu.learn.base.normaljdbc.service.impl.SimpleService;
import cn.cqu.edu.learn.base.normaljdbc.util.NormalJdbcUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NormalJdbcApplication {

    public static void main(String[] args) {
        SpringApplication.run(NormalJdbcApplication.class, args);
    }

}
