package cn.darkjrong.spring.boot.autoconfigure;

import cn.darkjrong.quartz.utils.QuartzUtils;
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
    public QuartzUtils quartzUtils(Scheduler scheduler) {
        return new QuartzUtils(scheduler);
    }

    @Bean
    @ConditionalOnMissingBean
    public SchedulerStarter schedulerStarter(Scheduler scheduler) {
        return new SchedulerStarter(scheduler);
    }

}
