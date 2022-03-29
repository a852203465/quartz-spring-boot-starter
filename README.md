# Quartz Spring Boot Starter

## 适用场景
基于quartz定时任务进行处理，旨在简化定时任务配置时的繁琐操作。

## 日常使用
 - 将下面的代码复制到POM文件中指定的位置
```xml
<dependency>
    <groupId>cn.darkjrong</groupId>
    <artifactId>quartz-spring-boot-starter</artifactId>
    <version>1.0</version>
</dependency>
```

- 工程中代码只需要在 `public method` 中加上指定注解即可
```java
@Service
public class CustomDemoJob {
    /**
     * 可以直接写表达式，也可以写配置文件里的key
     * 1/5 * * * * ?
     */
    @QuartzScheduled(cron = "${cron.expression.demo}", initialDelay = 1000 * 120)
    public void cronJob() throws Exception {
        System.out.println(Thread.currentThread() + "cronJob start " + new Date());
        Thread.sleep(10 * 1000);
        System.out.println(Thread.currentThread() + "cronJob end " + new Date());
    }  
    @QuartzScheduled(fixedDelay = 1000 * 5)
    public void fixedDealyJob() throws Exception {
        System.out.println(Thread.currentThread() + "fixedDealyJob start " + new Date());
        Thread.sleep(10 * 1000);
        System.out.println(Thread.currentThread() + "fixedDealyJob end " + new Date());  
    }
  
    @QuartzScheduled(fixedRate = 1000 * 5, initialDelay = 1000 * 120)
    public void fixedRateJob() throws Exception {
        System.out.println(Thread.currentThread() + "fixedRateJob start " + new Date());
        Thread.sleep(10 * 1000);
        System.out.println(Thread.currentThread() + "fixedRateJob end " + new Date());   
    }
    /**
    * 动态调整的定时任务
    * @param a 自定义参数 基本类型必须是包装类型
    * @param b 自定义参数 必须实现 java.io.Serializable 接口
    */
    public void task1(Integer a, String b) {
        System.out.printf("%d, %s\n", a, b);
    }
}
```

- 测试动态调整的定时任务
```java

@SpringBootTest
public class QuartzTest {

    @Autowired
    private QuartzTemplate quartzTemplate;

    @Test
    void addJob() throws InterruptedException {

        QuartzJobModule quartzJobModule = new QuartzJobModule();
        quartzJobModule.setCron("* * * * * ?");
        quartzJobModule.setJobClass(TaskJobDetail.class);

        quartzTemplate.addJob(quartzJobModule);
        Thread.sleep(1000 * 1000);
    }

    @Slf4j
    public static class TaskJobDetail extends QuartzJobBean {
        @Override
        protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
            log.info("begin delwith batch task >>>>>>>>>>>>>>>>>>>>>>>");
            String batchId = context.getJobDetail().getKey().getName();
            log.info("执行的任务id为：[{}]", batchId);
        }

    }

}
```








































