package cn.darkjrong.quartz.job;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 固定延迟的工作数据
 *
 * @author Rong.Jia
 * @date 2021/10/06
 */
public final class FixedDelayJobData implements Serializable {

    private static final long serialVersionUID = 1L;

    private long delay;

    private TimeUnit delayUnit;

    public FixedDelayJobData(long delay) {
        this(delay, TimeUnit.MILLISECONDS);
    }

    public FixedDelayJobData(long delay, final TimeUnit delayUnit) {
        if (delay == 0) {
            throw new IllegalArgumentException("Delay cannot be zero");
        }
        if (delayUnit == null) {
            throw new IllegalArgumentException("Delay Unit should be provided");
        }
        this.delay = delay;
        this.delayUnit = delayUnit;
    }

    public Date getNextScheduleDate() {
        return new Date(System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(delay, delayUnit));
    }

    @Override
    public String toString() {
        return "FixedDelayJobData [delay=" + delay + ", delayUnit=" + delayUnit + "]";
    }
}