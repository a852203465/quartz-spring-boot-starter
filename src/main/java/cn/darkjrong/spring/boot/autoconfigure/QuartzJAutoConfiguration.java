package cn.darkjrong.spring.boot.autoconfigure;

import cn.darkjrong.quartz.QuartzTemplate;
import org.quartz.Scheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * qz 自动配置类
 *
 * @author Rong.Jia
 * @date 2021/10/06
 */
@Configuration
public class QuartzJAutoConfiguration {

    @Bean
    public QuartzTemplate quartzTemplate(Scheduler scheduler) {
        return new QuartzTemplate(scheduler);
    }

    @Bean
    @ConditionalOnMissingBean
    public SchedulerStarter schedulerStarter(Scheduler scheduler) {
        return new SchedulerStarter(scheduler);
    }

}
