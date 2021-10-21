package cn.darkjrong.quartz.exceptions;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 调度器运行时异常
 *
 * @author Rong.Jia
 * @date 2021/10/21
 */
public class SchedulerRuntimeException extends RuntimeException {

    public SchedulerRuntimeException(Throwable e) {
        super(ExceptionUtil.getMessage(e), e);
    }

    public SchedulerRuntimeException(String message) {
        super(message);
    }

    public SchedulerRuntimeException(String messageTemplate, Object... params) {
        super(StrUtil.format(messageTemplate, params));
    }

    public SchedulerRuntimeException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public SchedulerRuntimeException(Throwable throwable, String messageTemplate, Object... params) {
        super(StrUtil.format(messageTemplate, params), throwable);
    }

    public boolean causeInstanceOf(Class<? extends Throwable> clazz) {
        Throwable cause = this.getCause();
        return null != clazz && clazz.isInstance(cause);
    }




















}
