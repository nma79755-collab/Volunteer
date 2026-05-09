package com.cyh.activityservice.Job;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.cyh.activityservice.mapper.ActivityMapper;
import com.cyh.entity.Activity;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class XXL_Activity_job {
    @Autowired
    private ActivityMapper activityMapper;
    @XxlJob("activityJobHandler") //与xxl-job-admin中web端的定时任务配置的jobHandler一致
    public void activityJobHandler() throws Exception {
        log.info("开始执行定时任务,清除过期的活动");
        activityMapper.update(new UpdateWrapper<Activity>().lt("end_time", LocalDateTime.now()).set("status", 3));
    }
    @XxlJob("activityStartJobHandler")
    public void activityStartJobHandler() throws Exception {
        log.info("开始执行定时任务,开始到时间的活动");
        activityMapper.update(new UpdateWrapper<Activity>().lt("start_time", LocalDateTime.now()).set("status", 2));
    }
}
