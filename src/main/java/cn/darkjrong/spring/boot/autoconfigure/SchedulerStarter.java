package cn.darkjrong.spring.boot.autoconfigure;

import cn.darkjrong.quartz.annotation.QuartzScheduled;
import cn.darkjrong.quartz.job.ClusterQuartzFixedDelayJobBean;
import cn.darkjrong.quartz.job.ClusterQuartzJobBean;
import cn.darkjrong.quartz.job.FixedDelayJobData;
import cn.darkjrong.quartz.job.FixedDelayJobListener;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.*;

/**
 * 调度程序启动器
 *
 * @author Rong.Jia
 * @date 2021/10/06
 */
public class SchedulerStarter implements BeanPostProcessor, ApplicationContextAware {

    private Instant now;
    private final Map<String, JobDetailTrigger> jobDetailTriggerMap = new HashMap<>();
    private ConfigurableApplicationContext applicationContext;
    private final Scheduler scheduler;

    public SchedulerStarter(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        now = Instant.now();
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Method[] methods = ReflectionUtils.getAllDeclaredMethods(bean.getClass());
        if (ArrayUtil.isNotEmpty(methods)) {
            for (Method method : methods) {
                QuartzScheduled annotation = AnnotationUtils.findAnnotation(method, QuartzScheduled.class);
                if (annotation != null) {

                    JobDataMap jobDataMap = new JobDataMap();
                    jobDataMap.put("targetObject", beanName);
                    jobDataMap.put("targetMethod", method.getName());

                    String cron = annotation.cron();
                    long fixedDelay = annotation.fixedDelay();
                    long fixedRate = annotation.fixedRate();
                    int initialDelay = (int) annotation.initialDelay();

                    final JobDetail jobDetail;
                    final Trigger trigger;

                    String jobDetailIdentity = beanName + "." + method.getName();

                    if (StrUtil.isNotBlank(cron)) {

                        cron = getCronExpression(cron);
                        jobDetail = JobBuilder.newJob(ClusterQuartzJobBean.class).storeDurably(true).usingJobData(jobDataMap).build();

                        trigger = TriggerBuilder.newTrigger().withIdentity(jobDetailIdentity)
                                .startAt(new Date(now.plusMillis(initialDelay).toEpochMilli()))
                                .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                                .build();

                    } else if (fixedDelay > 0) {

                        jobDataMap.put(FixedDelayJobListener.FIXED_DELAY_JOB_DATA, new FixedDelayJobData(fixedDelay));
                        jobDetail = JobBuilder.newJob(ClusterQuartzFixedDelayJobBean.class).storeDurably(true).usingJobData(jobDataMap).build();

                        trigger = TriggerBuilder.newTrigger().withIdentity(jobDetailIdentity)
                                .startAt(new Date(now.plusMillis(initialDelay).toEpochMilli()))
                                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(fixedDelay).repeatForever())
                                .build();
                    } else {

                        jobDetail = JobBuilder.newJob(ClusterQuartzJobBean.class).storeDurably(true).usingJobData(jobDataMap).build();

                        trigger = TriggerBuilder.newTrigger().withIdentity(jobDetailIdentity)
                                .startAt(new Date(now.plusMillis(initialDelay).toEpochMilli()))
                                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(fixedRate).repeatForever())
                                .build();
                    }

                    jobDetailTriggerMap.put(jobDetailIdentity, new JobDetailTrigger(jobDetail, trigger));
                }
            }
        }
        return bean;
    }

    /**
     * 得到cron表达式
     *
     * @param cron cron
     * @return {@link String}
     */
    private String getCronExpression(String cron) {
        cron = cron.trim();
        if (cron.startsWith("${") && cron.endsWith("}")) {
            return applicationContext.getBeanFactory().resolveEmbeddedValue(cron);
        }
        return cron;
    }

    /**
     * 执行调度器
     *
     * @param event 事件
     */
    @EventListener
    public void startScheduler(ContextRefreshedEvent event) {
        try {
            scheduler.deleteJobs(getJobKeys());
            scheduler.unscheduleJobs(getTriggerKeys());
            scheduler.getListenerManager().addJobListener(new FixedDelayJobListener());
            for (String jobDetailIdentity : jobDetailTriggerMap.keySet()) {
                JobDetailTrigger jobDetailTrigger = this.jobDetailTriggerMap.get(jobDetailIdentity);
                scheduler.scheduleJob(jobDetailTrigger.jobDetail, jobDetailTrigger.trigger);
            }
            if (!scheduler.isShutdown()) {
                scheduler.startDelayed(60);
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }


    /**
     * 获取数据库中的所有JobKey
     *
     * @return JobKey列表
     * @throws SchedulerException 调度程序异常
     */
    private List<JobKey> getJobKeys() throws SchedulerException {
        List<String> jobGroupNames = scheduler.getJobGroupNames();
        List<JobKey> jobKeys = new ArrayList<>();
        for (String jobGroupName : jobGroupNames) {
            jobKeys.addAll(scheduler.getJobKeys(GroupMatcher.jobGroupEquals(jobGroupName)));
        }
        return jobKeys;
    }

    /**
     * 获取数据库中的所有TriggerKey
     *
     * @return TriggerKey列表
     * @throws SchedulerException 调度程序异常
     */
    private List<TriggerKey> getTriggerKeys() throws SchedulerException {
        List<String> triggerGroupNames = scheduler.getJobGroupNames();
        List<TriggerKey> triggerKeys = new ArrayList<>();
        for (String triggerGroupName : triggerGroupNames) {
            triggerKeys.addAll(scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(triggerGroupName)));
        }
        return triggerKeys;
    }

    private static class JobDetailTrigger {

        JobDetail jobDetail;
        Trigger trigger;

        JobDetailTrigger(JobDetail jobDetail, Trigger trigger) {
            this.jobDetail = jobDetail;
            this.trigger = trigger;
        }

    }
}
