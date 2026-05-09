package com.cyh.activityservice.Service;

import com.cyh.activityservice.Dto.ActivityCreateRequest;
import com.cyh.activityservice.Dto.ActivityUpdateRequest;
import com.cyh.activityservice.enums.ActivityStatus;
import com.cyh.entity.Activity;
import com.cyh.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Activity Validator
 * 
 * Validates activity creation, update, and publish operations.
 */
@Slf4j
@Component
public class ActivityValidator {
    
    /**
     * Validate activity creation request
     * @param request the activity creation request
     * @throws BusinessException if validation fails
     */
    public void validateActivityCreate(ActivityCreateRequest request) {
        log.debug("Validating activity creation request");
        
        // Validate time logic
        if (request.getEndTime().isBefore(request.getStartTime())) {
            log.warn("End time is before start time");
            throw new BusinessException("结束时间不能早于开始时间");
        }
        
        // Validate start time is in the future
        if (request.getStartTime().isBefore(LocalDateTime.now())) {
            log.warn("Start time is in the past");
            throw new BusinessException("开始时间不能早于当前时间");
        }
    }
    
    /**
     * Validate activity update request
     * @param activity the existing activity
     * @param request the activity update request
     * @throws BusinessException if validation fails
     */
    public void validateActivityUpdate(Activity activity, ActivityUpdateRequest request) {
        log.debug("Validating activity update request for activity: {}", activity.getId());
        
        // Check activity status - only draft can be updated
        if (ActivityStatus.DRAFT.getCode() != activity.getStatus()) {
            log.warn("Cannot update activity with status: {}", activity.getStatus());
            throw new BusinessException("只能编辑草稿状态的活动");
        }
        
        // Validate time logic
        if (request.getEndTime().isBefore(request.getStartTime())) {
            log.warn("End time is before start time");
            throw new BusinessException("结束时间不能早于开始时间");
        }
        
        // Validate start time is in the future
        if (request.getStartTime().isBefore(LocalDateTime.now())) {
            log.warn("Start time is in the past");
            throw new BusinessException("开始时间不能早于当前时间");
        }
    }
    
    /**
     * Validate activity publish request
     * @param activity the activity to publish
     * @throws BusinessException if validation fails
     */
    public void validateActivityPublish(Activity activity) {
        log.debug("Validating activity publish request for activity: {}", activity.getId());
        
        // Check activity status - only draft can be published
        if (ActivityStatus.DRAFT.getCode() != activity.getStatus()) {
            log.warn("Cannot publish activity with status: {}", activity.getStatus());
            throw new BusinessException("只能发布草稿状态的活动");
        }
    }
}
