package cn.jk;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.stereotype.Component;

import cn.jk.annotation.QuartzScheduled;

@Component
public class QuartZTask {

    @QuartzScheduled(cron = "* * * * * ? ")
    public void run1() {
        System.out.println("run1:---" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
    }

    @QuartzScheduled(cron = "0/5 * * * * ? ")
    public void run2() {
        System.out.println("run22222:---" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
    }

    @QuartzScheduled(cron = "0/10 * * * * ? ")
    public void run3() {
        System.out.println("run333333333:---" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
    }

    @QuartzScheduled(cron = "0/15 * * * * ? ")
    public void run4() {
        System.out.println("run4444444444444444:---" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
    }
}
