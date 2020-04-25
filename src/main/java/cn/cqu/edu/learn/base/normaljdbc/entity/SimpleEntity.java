package cn.cqu.edu.learn.base.normaljdbc.entity;

public class SimpleEntity {

    // 类初始化阶段复制
    public static int staticInt = 520;

    // 类实例化赋值
    public int dynamicInt = 250;

    // 普通代码块类实例化阶段执行
    {
        System.err.println("没名字的代码块");
        System.err.println(this.dynamicInt);
    }

    // 静态代码块类初始化阶段执行
    static {
        System.err.println("静态代码块");
        System.err.println(staticInt);
    }

    // 方法一律不执行，除非主动调用
    public static void staticFun() {
        System.err.println("静态函数执行");
    }

    public void dynamicFun() {
        System.err.println("动态函数执行");
    }
}
