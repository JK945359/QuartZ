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
}
