package com.cyh.activityservice.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cyh.activityservice.enums.ActivityStatus;
import com.cyh.activityservice.mapper.ActivityMapper;
import com.cyh.entity.Activity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Activity Scheduled Task
 * 
 * Handles automatic status transitions for activities based on time:
 * - Changes PUBLISHED activities to IN_PROGRESS when startTime is reached
 * - Changes IN_PROGRESS activities to COMPLETED when endTime is reached
 */
@Slf4j
@Component
public class ActivityScheduledTask {
    
    @Autowired
    private ActivityMapper activityMapper;
    
    /**
     * Scheduled task to update activity statuses
     * Runs every minute (60000 milliseconds)
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void updateActivityStatuses() {
        log.debug("Starting scheduled task to update activity statuses");
        
        try {
            // Update PUBLISHED activities to IN_PROGRESS
            updatePublishedToInProgress();
            
            // Update IN_PROGRESS activities to COMPLETED
            updateInProgressToCompleted();
            
            log.debug("Scheduled task completed successfully");
        } catch (Exception e) {
            log.error("Error in scheduled task for activity status update", e);
        }
    }
    
    /**
     * Update PUBLISHED activities to IN_PROGRESS when startTime is reached
     */
    private void updatePublishedToInProgress() {
        log.debug("Checking for PUBLISHED activities to transition to IN_PROGRESS");
        
        LocalDateTime now = LocalDateTime.now();
        
        // Query for PUBLISHED activities where startTime <= now
        QueryWrapper<Activity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", ActivityStatus.PUBLISHED.getCode())
                .le("start_time", now)
                .eq("is_deleted", 0);
        
        List<Activity> activities = activityMapper.selectList(queryWrapper);
        
        if (activities.isEmpty()) {
            log.debug("No PUBLISHED activities to transition");
            return;
        }
        
        log.info("Found {} PUBLISHED activities to transition to IN_PROGRESS", activities.size());
        
        for (Activity activity : activities) {
            try {
                activity.setStatus(ActivityStatus.IN_PROGRESS.getCode());
                activity.setUpdateTime(LocalDateTime.now());
                activityMapper.updateById(activity);
                log.info("Activity {} transitioned from PUBLISHED to IN_PROGRESS", activity.getId());
            } catch (Exception e) {
                log.error("Error updating activity {} status", activity.getId(), e);
            }
        }
    }
    
    /**
     * Update IN_PROGRESS activities to COMPLETED when endTime is reached
     */
    private void updateInProgressToCompleted() {
        log.debug("Checking for IN_PROGRESS activities to transition to COMPLETED");
        
        LocalDateTime now = LocalDateTime.now();
        
        // Query for IN_PROGRESS activities where endTime <= now
        QueryWrapper<Activity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", ActivityStatus.IN_PROGRESS.getCode())
                .le("end_time", now)
                .eq("is_deleted", 0);
        
        List<Activity> activities = activityMapper.selectList(queryWrapper);
        
        if (activities.isEmpty()) {
            log.debug("No IN_PROGRESS activities to transition");
            return;
        }
        
        log.info("Found {} IN_PROGRESS activities to transition to COMPLETED", activities.size());
        
        for (Activity activity : activities) {
            try {
                activity.setStatus(ActivityStatus.COMPLETED.getCode());
                activity.setUpdateTime(LocalDateTime.now());
                activityMapper.updateById(activity);
                log.info("Activity {} transitioned from IN_PROGRESS to COMPLETED", activity.getId());
            } catch (Exception e) {
                log.error("Error updating activity {} status", activity.getId(), e);
            }
        }
    }
}
