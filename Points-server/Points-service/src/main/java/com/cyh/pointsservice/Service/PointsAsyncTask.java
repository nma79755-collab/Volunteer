package com.cyh.pointsservice.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.cyh.Client.ActivityClient;
import com.cyh.entity.Activity;
import com.cyh.enums.ActivityStatus;
import com.cyh.pointsservice.entity.Points;
import com.cyh.pointsservice.mapper.PointsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Points Async Task
 *
 * Provides scheduled trigger for asynchronous points distribution
 * to completed activities that haven't had points distributed yet.
 * Requirement: 5
 */
@Slf4j
@Component
public class PointsAsyncTask {

    @Autowired
    private PointsService pointsService;

    @Autowired
    private ActivityClient activityClient;

    @Autowired
    private PointsMapper pointsMapper;

    /**
     * Trigger async points distribution for a specific activity.
     *
     * @param activityId the activity ID
     */
    @Async("pointsTaskExecutor")
    public void distributePointsForCompletedActivity(Long activityId) {
        log.info("Triggering async points distribution for activity {}", activityId);
        pointsService.distributePointsAsync(activityId);
    }

    /**
     * Scheduled task to distribute points for completed activities.
     * Runs at 5 minutes past every hour.
     */
    @Scheduled(cron = "0 5 * * * ?")
    public void distributePointsForAllCompletedActivities() {
        log.debug("Starting scheduled task to distribute points for completed activities");

        try {

            List<Activity> completedActivities = activityClient.getActivitylist(ActivityStatus.COMPLETED.getCode(),0);

            if (completedActivities.isEmpty()) {
                log.debug("No completed activities found for points distribution");
                return;
            }

            log.info("Found {} completed activities to check for points distribution", completedActivities.size());

            for (Activity activity : completedActivities) {
                try {
                    // Check if points have already been distributed for this activity
                    QueryWrapper<Points> pointsQw = new QueryWrapper<>();
                    pointsQw.eq("activity_id", activity.getId())
                            .eq("is_deleted", 0);
                    Long existingCount = pointsMapper.selectCount(pointsQw);

                    if (existingCount > 0) {
                        log.debug("Points already distributed for activity {}, skipping", activity.getId());
                        continue;
                    }

                    log.info("Triggering points distribution for activity {}", activity.getId());
                    distributePointsForCompletedActivity(activity.getId());
                } catch (Exception e) {
                    log.error("Error triggering points distribution for activity {}: {}", activity.getId(), e.getMessage());
                }
            }

            log.debug("Scheduled points distribution task completed");
        } catch (Exception e) {
            log.error("Error in scheduled task for points distribution", e);
        }
    }
}
