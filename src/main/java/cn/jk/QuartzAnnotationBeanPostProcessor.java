package cn.jk;

import cn.jk.annotation.QuartzScheduled;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
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

import java.util.*;

/**
 * 自定义注解初始化加载QuartZ定时任务
 *
 * @author JK
 * @date 2019/10/18
 */
@Slf4j
public class QuartzAnnotationBeanPostProcessor
        implements BeanPostProcessor, ApplicationContextAware, ApplicationListener<ContextRefreshedEvent>, DisposableBean {

    @Value("${quartz.jdbc.url}")
    private String url;
    @Value("${quartz.jdbc.username}")
    private String username;
    @Value("${quartz.jdbc.password}")
    private String password;

    /**
     * 需要获取真实bean的类名
     */
    private List<String> beanNameList = Arrays.asList(QuartZTask.class.getName());

    // 触发器集合
    private List<Trigger> triggers = new ArrayList<Trigger>();

    private ConfigurableApplicationContext applicationContext;

    private DefaultListableBeanFactory defaultListableBeanFactory;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
        defaultListableBeanFactory = (DefaultListableBeanFactory) this.applicationContext.getBeanFactory();
    }

    // bean初始化方法调用前被调用
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    // bean初始化方法调用后被调用,bean代表对象，beanName代表对象名，bean的id
    @SneakyThrows
    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
        //如果bean对象使用@Transactional注解，spring会代理bean对象，
        // spring ioc容器中的bean是代理类，要获取自定义注解需要通道如下方式获取
        Object target = bean;
        //必须增加条件，不是所有的类都可以获取具体bean对象
   /*     Advised advised = (Advised) bean;
        SingletonTargetSource singletonTargetSource = (SingletonTargetSource) advised.getTargetSource();
        target = singletonTargetSource.getTarget();*/
        String simpleBeanName = bean.getClass().getSimpleName();
        // 过滤没有@QuartzScheduled注解和有参数的方法
        Arrays.asList(target.getClass().getDeclaredMethods()).stream()
                .filter(m -> m.getAnnotation(QuartzScheduled.class) != null && m.getParameterCount() == 0).forEach(m -> {
            String mname = m.getName();
            String beanMethodName = simpleBeanName + StringUtils.capitalize(mname);
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
            jobDetailBuilder.addPropertyValue("group", StringUtils.capitalize(simpleBeanName) + "JobDetail");
            defaultListableBeanFactory.registerBeanDefinition(jobBeanName, jobDetailBuilder.getBeanDefinition());

            BeanDefinitionBuilder triggerBuilder =
                    BeanDefinitionBuilder.genericBeanDefinition(CronTriggerFactoryBean.class);
            JobDetail jobDetail = applicationContext.getBean(jobBeanName, JobDetail.class);
            triggerBuilder.addPropertyValue("jobDetail", jobDetail);
            triggerBuilder.addPropertyValue("cronExpression", m.getAnnotation(QuartzScheduled.class).cron());
            triggerBuilder.addPropertyValue("name", mname + "Trigger");
            triggerBuilder.addPropertyValue("group", StringUtils.capitalize(simpleBeanName) + "Trigger");
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
                    // builder.addPropertyValue("quartzProperties", quartzProperties());
                    String beanName = "schedulerFactoryBean";
                    defaultListableBeanFactory.registerBeanDefinition(beanName, builder.getBeanDefinition());
                    // applicationContext.getBean(beanName, Scheduler.class).start();
                    applicationContext.getBean(beanName, Scheduler.class).startDelayed(10);// spring容器启动后10秒再启动Scheduler
                }
                triggers.clear();
            }
        } catch (Exception e) {
            log.info("加载调度器失败，" + e.getMessage(), e);
        }
    }

    @Override
    public void destroy() throws Exception {
        // 为了防止QuartZ工作线程没有关闭，造成内存泄漏，暂时这样解决
        Thread.sleep(2000);
    }

    private Properties quartzProperties() {
        Properties prop = new Properties();
        prop.put("org.quartz.scheduler.instanceName", "quartZTaskScheduler");
        prop.put("org.quartz.scheduler.instanceId", "AUTO");
        prop.put("org.quartz.scheduler.skipUpdateCheck", "true");

        prop.put("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
        prop.put("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        prop.put("org.quartz.jobStore.tablePrefix", "QRTZ_");
        prop.put("org.quartz.jobStore.isClustered", "true");
        prop.put("org.quartz.jobStore.dataSource", "quartZ");
        prop.put("org.quartz.jobStore.maxMisfiresToHandleAtATime", "3");
        prop.put("org.quartz.scheduler.skipUpdateCheck", "true");
        prop.put("org.quartz.jobStore.clusterCheckinInterval", "2000");
        prop.put("org.quartz.jobStore.misfireThreshold", "60000");

        prop.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        prop.put("org.quartz.threadPool.threadCount", "10");
        prop.put("org.quartz.threadPool.threadPriority", "5");

        prop.put("org.quartz.dataSource.quartZ.driver", "com.mysql.jdbc.Driver");
        prop.put("org.quartz.dataSource.quartZ.URL", url);
        prop.put("org.quartz.dataSource.quartZ.user", username);
        prop.put("org.quartz.dataSource.quartZ.password", password);
        prop.put("org.quartz.dataSource.quartZ.maxConnections", "20");
        prop.put("org.quartz.dataSource.quartZ.validationQuery", "select 0");

        prop.put("org.quartz.plugin.shutdownhook.class", "org.quartz.plugins.management.ShutdownHookPlugin");
        prop.put("org.quartz.plugin.shutdownhook.cleanShutdown", "true");
        return prop;
    }
}
