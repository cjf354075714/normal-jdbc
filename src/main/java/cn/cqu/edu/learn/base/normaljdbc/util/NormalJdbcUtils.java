package cn.cqu.edu.learn.base.normaljdbc.util;


import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component("normalJdbcUtils")
public class NormalJdbcUtils implements ApplicationContextAware {

    private static ApplicationContext context = null;

    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        NormalJdbcUtils.context = applicationContext;
    }

    @NotNull
    public static <T> T getBean(Class<T> classType) throws Exception {
        return NormalJdbcUtils.context.getBean(classType);
    }
}
