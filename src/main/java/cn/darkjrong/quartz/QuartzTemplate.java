package cn.darkjrong.quartz;

import cn.darkjrong.quartz.domain.QuartzJobModule;
import cn.darkjrong.quartz.exceptions.SchedulerRuntimeException;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.lang.Assert;
import lombok.AllArgsConstructor;
import org.quartz.*;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * qz工具类
 *
 * @author Rong.Jia
 * @date 2021/10/05
 */
@AllArgsConstructor
public class QuartzTemplate {

    public static final String DEFAULT_JOB = "defaultJob";
    public static final String DEFAULT_GROUP = "defaultGroup";

    private static final Logger log = LoggerFactory.getLogger(QuartzTemplate.class);
    private final Scheduler scheduler;

    /**
     * 启动定时任务
     *
     * @throws SchedulerException 调度程序异常
     */
    private void start() throws SchedulerException {
        if (!scheduler.isShutdown()) {
            scheduler.start();
        }
    }

    /**
     * 启动延迟
     *
     * @param seconds 秒
     * @throws SchedulerRuntimeException 调度器运行时异常
     */
    public void startDelayed(int seconds) throws SchedulerRuntimeException {
        try {
            if (!scheduler.isShutdown()) {
                scheduler.startDelayed(seconds);
            }
        }catch (Exception e) {
            log.error("startDelayed {}", e.getMessage());
            throw new SchedulerRuntimeException(e);
        }
    }

    /**
     * 检查是否存在
     *
     * @param jobName 作业名
     * @return {@link Boolean}
     */
    public Boolean checkExists(String jobName) {
        return this.checkExists(DEFAULT_JOB, jobName);
    }

    /**
     * 检查是否存在
     * @param groupName 分组名
     * @param jobName 作业名
     * @return {@link Boolean}
     */
    public Boolean checkExists(String groupName, String jobName) {
        JobKey jobKey = JobKey.jobKey(jobName, groupName);
        try {
            return scheduler.checkExists(jobKey);
        }catch (Exception e) {
            log.error("Check task {} is abnormal. Exception information {} is abnormal", jobName, e.getMessage());
        }
        return Boolean.FALSE;
    }

    /**
     * 是否是启动的
     *
     * @return {@link Boolean}
     */
    public Boolean isStarted() {
        try {
            return scheduler.isStarted();
        }catch (SchedulerException e) {
            log.error("isStarted {}", e.getMessage());
        }

        return Boolean.FALSE;
    }

    /**
     * 中断任务
     *
     * @param groupName 组名称
     * @param jobName   作业名
     * @return {@link Boolean}
     */
    public Boolean interrupt(String groupName, String jobName) {
        JobKey jobKey = JobKey.jobKey(jobName, groupName);
        try {
            return scheduler.interrupt(jobKey);
        }catch (Exception e) {
            log.error("Interrupted task {} Is abnormal. The cause is {}", jobName, e.getMessage());
        }
        return Boolean.FALSE;
    }

    /**
     * 清除所有任务
     *
     * @return {@link Boolean}
     */
    public Boolean clear() {
        try {
            scheduler.clear();
            return Boolean.TRUE;
        } catch (SchedulerException e) {
            log.error("clear {}", e.getMessage());
        }
        return Boolean.FALSE;
    }

    /**
     * 暂时停止Scheduler's触发{@link Trigger}s
     *
     * @return {@link Boolean}
     */
    public Boolean standby(){
        try {
            scheduler.standby();
            return Boolean.TRUE;
        }catch (SchedulerException e) {
            log.error("standby {}", e.getMessage());
        }

        return Boolean.FALSE;
    }

    /**
     * 添加一个定时任务
     *
     * @param quartzModel 任务对象
     * @return {@link Boolean}
     */
    public Boolean addJob(QuartzJobModule quartzModel) {

        Assert.isTrue(quartzModel.verify(), "QuartzModel is invalid!");

        try {
            JobDetail job = JobBuilder.newJob(quartzModel.getJobClass())
                    .withIdentity(quartzModel.getJobName(), quartzModel.getGroupName())
                    .setJobData(quartzModel.getJobDataMap()).build();

            // 表达式调度构建器
            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(quartzModel.getCron());

            // 按新的cronExpression表达式构建一个新的trigger
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(quartzModel.getJobName(), quartzModel.getGroupName())
                    .startAt(quartzModel.getStartTime()).endAt(quartzModel.getEndTime())
                    .withSchedule(scheduleBuilder)
                    .build();

            scheduler.scheduleJob(job, trigger);
            start();
            return Boolean.TRUE;
        } catch (SchedulerException e) {
            log.error("Add quartz job error, jobName => {}, error info => {}", quartzModel.getJobName(), e.getMessage());
        }

        return Boolean.FALSE;
    }

    /**
     * 修改一个任务的触发时间(使用默认的任务组名 ， 触发器名 ， 触发器组名)
     *
     * @param jobName   任务名
     * @param cron      cron表达式
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return {@link Boolean}
     */
    public Boolean modifyJobTime(String jobName, String cron, Date startDate, Date endDate) {

        TriggerKey triggerKey = TriggerKey.triggerKey(jobName, DEFAULT_GROUP);

        try {

            CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
            String oldTime = trigger.getCronExpression();
            if (!oldTime.equalsIgnoreCase(cron)) {
                CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cron);
                // 按新的cronExpression表达式重新构建trigger
                trigger = trigger.getTriggerBuilder().withIdentity(triggerKey)
                        .startAt(startDate).endAt(endDate).withSchedule(scheduleBuilder).build();
                // 按新的trigger重新设置job执行
                scheduler.rescheduleJob(triggerKey, trigger);
            }
        } catch (Exception e) {
            log.error("update quartz job time error, jobName => {}, error info => {}", jobName, e.getMessage());
        }

        return Boolean.FALSE;
    }

    /**
     * 修改任务，（可以修改任务名，任务类，触发时间） 原理：移除原来的任务，添加新的任务
     *
     * @param oldJobName ：原任务名
     * @param jobName    作业名
     * @param jobClass   执行定时任务的具体操作
     * @param cron       cron
     * @return {@link Boolean}
     */
    public Boolean modifyJob(String oldJobName, String jobName, Class<? extends Job> jobClass, String cron) {

        TriggerKey triggerKey = TriggerKey.triggerKey(oldJobName, DEFAULT_GROUP);
        JobKey jobKey = JobKey.jobKey(oldJobName, DEFAULT_JOB);
        try {
            Trigger trigger = scheduler.getTrigger(triggerKey);

            // 停止触发器, 移除触发器, 删除任务
            scheduler.pauseTrigger(triggerKey);
            scheduler.unscheduleJob(triggerKey);
            scheduler.deleteJob(jobKey);

            JobDetail job = JobBuilder.newJob(jobClass).withIdentity(jobName, DEFAULT_JOB).build();

            // 表达式调度构建器
            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cron);

            // 按新的cronExpression表达式构建一个新的trigger
            Trigger newTrigger = TriggerBuilder.newTrigger().withIdentity(jobName, DEFAULT_GROUP).withSchedule(scheduleBuilder).build();

            // 交给scheduler去调度
            scheduler.scheduleJob(job, newTrigger);

            // 启动
            start();
            return Boolean.TRUE;
        } catch (Exception e) {
            log.error("修改任务【{}】为: {} error, error info {}", oldJobName, jobName, e.getMessage());
        }
        return Boolean.FALSE;
    }

    /**
     * 修改一个任务的触发时间
     *
     * @param jobName   任务名
     * @param groupName 组名称
     * @param cron      cron
     * @return {@link Boolean}
     */
    public Boolean modifyJobTime(String jobName, String groupName, String cron) {

        TriggerKey triggerKey = TriggerKey.triggerKey(jobName, groupName);
        try {
            CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
            String oldTime = trigger.getCronExpression();

            if (!oldTime.equalsIgnoreCase(cron)) {

                // trigger已存在，则更新相应的定时设置
                CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cron);

                // 按新的cronExpression表达式重新构建trigger
                trigger = trigger.getTriggerBuilder().withIdentity(triggerKey).withSchedule(scheduleBuilder).build();

                // 按新的trigger重新设置job执行
                scheduler.resumeTrigger(triggerKey);
            }
            return Boolean.TRUE;
        } catch (Exception e) {
            log.error("update quartz job time error, jobName => {}, error info => {}", jobName, e.getMessage());
        }
        return Boolean.FALSE;
    }

    /**
     * 移除一个任务(使用默认的任务组名 ， 触发器名 ， 触发器组名)
     *
     * @param jobName 作业名
     * @return {@link Boolean}
     */
    public Boolean removeJob(String jobName) {
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName, DEFAULT_GROUP);
        JobKey jobKey = JobKey.jobKey(jobName, DEFAULT_JOB);
        try {
            scheduler.pauseTrigger(triggerKey);
            scheduler.unscheduleJob(triggerKey);
            scheduler.deleteJob(jobKey);
            return Boolean.TRUE;
        } catch (Exception e) {
            log.error("update quartz job error, jobName => {}, error info => {}", jobName, e.getMessage());
        }
        return Boolean.FALSE;
    }

    /**
     * 移除一个任务
     *
     * @param jobName   任务名
     * @param groupName 组名
     * @return {@link Boolean}
     */
    public Boolean removeJob(String jobName, String groupName) {
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName, groupName);
        JobKey jobKey = JobKey.jobKey(jobName, groupName);
        try {
            scheduler.pauseTrigger(triggerKey);
            scheduler.unscheduleJob(triggerKey);
            scheduler.deleteJob(jobKey);
            return Boolean.FALSE;
        } catch (Exception e) {
            log.error("remove quartz job error, jobName => {}, error info => {}", jobName, e.getMessage());
        }
        return Boolean.FALSE;
    }

    /**
     * 暂停一个任务(使用默认组名)
     *
     * @param jobName 任务名
     * @return {@link Boolean}
     */
    public Boolean pauseJob(String jobName) {
       return pauseJob(jobName, DEFAULT_JOB);
    }

    /**
     * 暂停一个任务
     *
     * @param jobName   作业名
     * @param groupName 组名称
     * @return {@link Boolean}
     */
    public Boolean pauseJob(String jobName, String groupName) {

        JobKey jobKey = JobKey.jobKey(jobName, groupName);
        try {
            scheduler.pauseJob(jobKey);
            return Boolean.TRUE;
        } catch (SchedulerException e) {
            log.error("pause quartz job error, jobName => {}, error info => {}", jobName, e.getMessage());
        }
        return Boolean.FALSE;
    }

    /**
     * 恢复一个任务(使用默认组名)
     *
     * @param jobName 任务名
     * @return {@link Boolean}
     */
    public Boolean resumeJob(String jobName) {
       return resumeJob(jobName, DEFAULT_JOB);
    }

    /**
     * 恢复一个任务
     *
     * @param jobName      作业名
     * @param groupName 工作的组名
     * @return {@link Boolean}
     */
    public Boolean resumeJob(String jobName, String groupName) {
        JobKey jobKey = JobKey.jobKey(jobName, groupName);
        try {
            scheduler.resumeJob(jobKey);
            return Boolean.TRUE;
        } catch (SchedulerException e) {
            log.error("resume quartz job error, jobName => {}, error info => {}", jobName, e.getMessage());
        }
        return Boolean.FALSE;
    }

    /**
     * 启动所有定时任务
     * @return {@link Boolean}
     */
    public Boolean startJobs() {
        try {
            scheduler.start();
            return Boolean.TRUE;
        } catch (Exception e) {
            log.error("All scheduled tasks fail to be started {}", e.getMessage());
        }
        return Boolean.FALSE;
    }

    /**
     * 关闭所有定时任务
     *
     * @return {@link Boolean}
     */
    public Boolean shutdownJobs() {
        try {
            if (!scheduler.isShutdown()) {
                scheduler.shutdown();
                return Boolean.TRUE;
            }
        } catch (Exception e) {
            log.error("Description Stopping all scheduled tasks is abnormal {}", e.getMessage());
        }
        return Boolean.FALSE;
    }

    /**
     * 是否是停止
     *
     * @return {@link Boolean}
     * @throws SchedulerException 调度程序异常
     */
    public Boolean isShutdown() throws SchedulerException {
        return scheduler.isShutdown();
    }

    /**
     * 获取目前执行的任务
     *
     * @return {@link List}<{@link JobExecutionContext}>
     * @throws SchedulerRuntimeException 调度器运行时异常
     */
    public List<JobExecutionContext> getCurrentlyExecutingJobs() throws SchedulerRuntimeException {
        try {
            return scheduler.getCurrentlyExecutingJobs();
        } catch (SchedulerException e) {
            log.error("getCurrentlyExecutingJobs {}", e.getMessage());
            throw new SchedulerRuntimeException(e);
        }
    }

    /**
     * 获取侦听器管理器
     *
     * @return {@link ListenerManager}
     * @throws SchedulerRuntimeException 调度器运行时异常
     */
    public ListenerManager getListenerManager() throws SchedulerRuntimeException {
        try {
           return scheduler.getListenerManager();
        } catch (SchedulerException e) {
            log.error("getListenerManager {}", e.getMessage());
            throw new SchedulerRuntimeException(e);
        }
    }

    /**
     * 立即运行任务，这里的立即运行，只会运行一次
     *
     * @param jobName 作业名
     * @return {@link Boolean}
     */
    public Boolean triggerJob(String jobName) {
        return triggerJob(jobName, DEFAULT_JOB);
    }

    /**
     * 立即运行任务，这里的立即运行，只会运行一次，方便测试时用。
     *
     * @param jobName      作业名
     * @param groupName 工作的组名
     * @return {@link Boolean}
     */
    public Boolean triggerJob(String jobName, String groupName) {
        JobKey jobKey = JobKey.jobKey(jobName, groupName);
        try {
            scheduler.triggerJob(jobKey);
            return Boolean.TRUE;
        } catch (SchedulerException e) {
            log.error("trigger quartz job error, jobName => {}, error info => {}", jobName, e.getMessage());
        }
        return Boolean.FALSE;
    }

    /**
     * 获取任务状态
     *
     * @param jobName 触发器名
     * @return {@link String}
     */
    public String getTriggerState(String jobName) {
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName, DEFAULT_GROUP);
        String name = null;
        try {
            Trigger.TriggerState triggerState = scheduler.getTriggerState(triggerKey);
            name = triggerState.name();
        } catch (SchedulerException e) {
            log.error("Error obtaining task status, jobName => {}, error info => {}", jobName, e.getMessage());
        }
        return name;
    }

    /**
     * 获取最近5次执行时间
     *
     * @param cron cron
     * @return {@link List}<{@link String}>
     */
    public List<String> getRecentTriggerTime(String cron) {
        List<String> list = new ArrayList<>();
        try {
            CronTriggerImpl cronTriggerImpl = new CronTriggerImpl();
            cronTriggerImpl.setCronExpression(cron);
            List<Date> dates = TriggerUtils.computeFireTimes(cronTriggerImpl, null, 5);
            for (Date date : dates) {
                list.add(DatePattern.PURE_DATETIME_FORMAT.format(date));

            }
        } catch (ParseException e) {
            log.error("GetRecentTriggerTime error, cron => {},  error info => {}", cron, e.getMessage());
        }
        return list;
    }


}
