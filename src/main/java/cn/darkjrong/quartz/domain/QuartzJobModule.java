package cn.darkjrong.quartz.domain;

import cn.darkjrong.quartz.QuartzTemplate;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.util.ClassUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 定时任务信息
 *
 * @author Rong.Jia
 * @date 2021/10/05
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuartzJobModule implements Serializable {

    private static final long serialVersionUID = -3134041332944687721L;

    /**
     * 触发器开始时间, 默认：当前时间
     */
    private Date startTime;

    /**
     * 触发器结束时间， 默认：时间+1月
     */
    private Date endTime;

    /**
     * 任务名, 默认：defaultJob
     */
    private String jobName;

    /**
     * 任务组名, 默认：defaultGroup
     */
    private String groupName;

    /**
     * 执行定时任务的具体操作
     */
    private Class<? extends Job> jobClass;

    /**
     * cron表达式
     */
    private String cron;

    /**
     * job的附加信息
     */
    private JobDataMap jobDataMap = new JobDataMap();

    public Date getStartTime() {
        return Validator.isNull(startTime) ? DateUtil.date() : startTime;
    }

    public String getJobName() {
        return StrUtil.isEmpty(jobName) ? QuartzTemplate.DEFAULT_JOB : jobName;
    }

    public String getGroupName() {
        return StrUtil.isEmpty(groupName) ? QuartzTemplate.DEFAULT_GROUP : groupName;
    }

    /**
     * 验证
     *
     * @return boolean
     */
    public boolean verify() {
        return (StrUtil.isNotBlank(getJobName())
                && StrUtil.isNotBlank(getGroupName())
                && StrUtil.isNotBlank(cron)
                && ObjectUtil.isNotEmpty(getStartTime())
                && (ClassUtils.hasMethod(Job.class, "execute", JobExecutionContext.class)
                || ClassUtils.hasMethod(Job.class, "executeInternal", JobExecutionContext.class))
        );
    }


}
