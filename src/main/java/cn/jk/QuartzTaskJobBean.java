package cn.jk;

import java.lang.reflect.Method;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 定时任务 注解方式执行调度
 * 
 * @author JK
 * @date 2019/10/18
 */
@Slf4j
public class QuartzTaskJobBean extends QuartzJobBean {

    private String targetObject;

    private String targetMethod;

    private ApplicationContext ctx;

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.ctx = applicationContext;
    }

    public void setTargetObject(String targetObject) {
        this.targetObject = targetObject;
    }

    public void setTargetMethod(String targetMethod) {
        this.targetMethod = targetMethod;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Object otargetObject = null;
        boolean beanHave = true;
        try {
            otargetObject = ctx.getBean(targetObject);
        } catch (Exception e) {
            beanHave = false;
        }
        try {
            // 类名修改后将不存在的触发器和任务移除和删除
            if (!beanHave) {
                /* Scheduler scheduler = context.getScheduler();
                scheduler.unscheduleJob(context.getTrigger().getKey());// 移除触发器
                scheduler.deleteJob(context.getJobDetail().getKey());// 删除任务
                */
                log.info("当前应用中" + StringUtils.capitalize(targetObject) + "类不存在");
                return;
            }
            boolean methodHave = false;
            Method[] methods = otargetObject.getClass().getMethods();
            for (Method method : methods) {
                if (method.getName().equalsIgnoreCase(targetMethod)) {
                    methodHave = true;
                    break;
                }
            }
            // 方法名修改后将不存在的触发器和任务移除和删除
            if (!methodHave) {
                /*  Scheduler scheduler = context.getScheduler();
                scheduler.unscheduleJob(context.getTrigger().getKey());// 移除触发器
                scheduler.deleteJob(context.getJobDetail().getKey());// 删除任务
                */
                log.info("当前应用中" + StringUtils.capitalize(targetObject) + "." + targetMethod + "()方法不存在");
                return;
            }
            Method m = otargetObject.getClass().getMethod(targetMethod, new Class[] {});// 无参数方法
            m.invoke(otargetObject, new Object[] {});
        } catch (Exception e) {
            log.info("JobDetail执行定时任务失败，" + e.getMessage(), e);
            throw new JobExecutionException(e);
        }
    }

}
