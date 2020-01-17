package cn.jk;

import cn.jk.annotation.QuartzScheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class QuartZTask {

    @QuartzScheduled(cron = "* * * * * ? ")
    public void run1() {
        System.out.println("run1:---" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
    }
}
