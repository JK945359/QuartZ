package cn.jk.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注解在方法上的Quartz Cron注解 暂时只支持无参方法，有参数的方法不会执行
 * 
 * @author JK
 * @date 2019/10/18
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface QuartzScheduled {

    /**
     * cron时间表达式
     * 
     * @return
     */
    String cron();
}
