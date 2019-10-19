package cn.jk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.util.StringUtils;

import cn.jk.annotation.QuartzScheduled;
import lombok.extern.slf4j.Slf4j;

/**
 * 自定义注解初始化加载QuartZ定时任务
 * 
 * @author JK
 * @date 2019/10/18
 */
@Slf4j
public class QuartzAnnotationBeanPostProcessor
    implements BeanPostProcessor, ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

    // 触发器集合
    private List<Trigger> triggers = new ArrayList<Trigger>();

    private ConfigurableApplicationContext applicationContext;

    private DefaultListableBeanFactory defaultListableBeanFactory;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext)applicationContext;
        defaultListableBeanFactory = (DefaultListableBeanFactory)this.applicationContext.getBeanFactory();
    }

    // bean初始化方法调用前被调用
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    // bean初始化方法调用后被调用
    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
        Arrays.asList(bean.getClass().getDeclaredMethods()).stream()
            .filter(m -> m.getAnnotation(QuartzScheduled.class) != null && m.getParameterCount() == 0).forEach(m -> {// 过滤没有@QuartzScheduled注解和有参数的方法
                String mname = m.getName();
                String beanMethodName = beanName + StringUtils.capitalize(mname);
                String jobBeanName = beanMethodName + "JobDetail";
                String triggerBeanName = beanMethodName + "Trigger";

                BeanDefinitionBuilder jobDetailBuilder =
                    BeanDefinitionBuilder.genericBeanDefinition(JobDetailFactoryBean.class);
                jobDetailBuilder.addPropertyValue("durability", true);
                jobDetailBuilder.addPropertyValue("jobClass", QuartzTaskJobBean.class);
                Map<String, String> jobDataAsMap = new HashMap<>();
                jobDataAsMap.put("targetObject", beanName);
                jobDataAsMap.put("targetMethod", mname);
                jobDetailBuilder.addPropertyValue("jobDataAsMap", jobDataAsMap);
                jobDetailBuilder.addPropertyValue("name", mname + "JobDetail");
                jobDetailBuilder.addPropertyValue("group", StringUtils.capitalize(beanName) + "JobDetail");
                defaultListableBeanFactory.registerBeanDefinition(jobBeanName, jobDetailBuilder.getBeanDefinition());

                BeanDefinitionBuilder triggerBuilder =
                    BeanDefinitionBuilder.genericBeanDefinition(CronTriggerFactoryBean.class);
                JobDetail jobDetail = applicationContext.getBean(jobBeanName, JobDetail.class);
                triggerBuilder.addPropertyValue("jobDetail", jobDetail);
                triggerBuilder.addPropertyValue("cronExpression", m.getAnnotation(QuartzScheduled.class).cron());
                triggerBuilder.addPropertyValue("name", mname + "Trigger");
                triggerBuilder.addPropertyValue("group", StringUtils.capitalize(beanName) + "Trigger");
                defaultListableBeanFactory.registerBeanDefinition(triggerBeanName, triggerBuilder.getBeanDefinition());

                triggers.add(applicationContext.getBean(triggerBeanName, Trigger.class));
            });
        return bean;
    }

    // spring容器初始化完毕调用
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            if (event.getApplicationContext() == this.applicationContext) {
                if (!triggers.isEmpty()) {
                    BeanDefinitionBuilder builder =
                        BeanDefinitionBuilder.genericBeanDefinition(SchedulerFactoryBean.class);
                    builder.addPropertyValue("triggers", triggers.toArray());
                    builder.addPropertyValue("overwriteExistingJobs", true);
                    builder.addPropertyValue("applicationContextSchedulerContextKey", "applicationContext");
                    builder.addPropertyValue("configLocation", "classpath:quartz.properties");
                    String beanName = "scheduledExecutorFactoryBean";
                    defaultListableBeanFactory.registerBeanDefinition(beanName, builder.getBeanDefinition());
                    applicationContext.getBean(beanName, Scheduler.class).start();
                }
            }
        } catch (Exception e) {
            log.info("加载调度器失败，" + e.getMessage(), e);
        }
    }

}
