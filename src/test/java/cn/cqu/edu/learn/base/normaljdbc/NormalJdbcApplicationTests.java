package cn.cqu.edu.learn.base.normaljdbc;

import cn.cqu.edu.learn.base.normaljdbc.service.ISimpleService;
import cn.cqu.edu.learn.base.normaljdbc.service.impl.SimpleService;
import cn.cqu.edu.learn.base.normaljdbc.util.NormalJdbcUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class NormalJdbcApplicationTests {

    @Test
    void contextLoads() {
        try {
            ISimpleService simpleService = NormalJdbcUtils.getBean(SimpleService.class);
            simpleService.printCurrentClassLoader();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
