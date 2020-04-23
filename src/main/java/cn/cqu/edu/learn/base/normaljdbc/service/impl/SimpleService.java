package cn.cqu.edu.learn.base.normaljdbc.service.impl;

import cn.cqu.edu.learn.base.normaljdbc.service.ISimpleService;
import org.springframework.stereotype.Service;

@Service("simpleService")
public class SimpleService implements ISimpleService {


    /**
     * 当前的类加载器，类加载器分为三类，这里是最低等级的应用加载器
     *
     * 其他等级的加载器先不看
     * @throws Exception 异常
     */
    @Override
    public void printCurrentClassLoader() throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        System.err.println(classLoader.toString());
    }

    /**
     * Class.forName 到底是什么方法
     * 首先，虚拟机去获取、执行一个类的步骤有哪几步？
     * 加载->链接->初始化->使用->卸载
     *
     * @throws Exception 异常
     */
    @Override
    public void loadDriver() throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
    }
}
