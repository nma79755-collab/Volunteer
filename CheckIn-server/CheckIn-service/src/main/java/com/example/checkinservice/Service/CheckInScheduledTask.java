package com.example.checkinservice.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cyh.Client.ActivityClient;
import com.cyh.entity.Activity;
import com.cyh.enums.ActivityStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Check-in Scheduled Task
 *
 * Automatically marks absent for volunteers who did not check in
 * after an activity has ended.
 * Requirement: 4
 */
@Slf4j
@Component
public class CheckInScheduledTask {

    @Autowired
    private ActivityClient activityClient;

    @Autowired
    private CheckInService checkInService;

    /**
     * Scheduled task to mark absent for completed activities.
     * Runs every 5 minutes (300000 milliseconds).
     */
    @Scheduled(fixedRate = 300000)
    public void markAbsentForCompletedActivities() {
        log.debug("Starting scheduled task to mark absent for completed activities");

        try {
            // Find all COMPLETED activities
            QueryWrapper<Activity> qw = new QueryWrapper<>();
            qw.eq("status", ActivityStatus.COMPLETED.getCode())
                    .eq("is_deleted", 0);

            List<Activity> completedActivities = activityClient.getActivitylist( ActivityStatus.COMPLETED.getCode(),0);

            if (completedActivities.isEmpty()) {
                log.debug("No completed activities found for absent marking");
                return;
            }

            log.info("Found {} completed activities to process for absent marking", completedActivities.size());

            for (Activity activity : completedActivities) {
                try {
                    int count = checkInService.markAbsent(activity.getId());
                    if (count > 0) {
                        log.info("Marked {} volunteers as absent for activity {}", count, activity.getId());
                    }
                } catch (Exception e) {
                    log.error("Error marking absent for activity {}: {}", activity.getId(), e.getMessage());
                }
            }

            log.debug("Scheduled absent marking task completed");
        } catch (Exception e) {
            log.error("Error in scheduled task for absent marking", e);
        }
    }
}
