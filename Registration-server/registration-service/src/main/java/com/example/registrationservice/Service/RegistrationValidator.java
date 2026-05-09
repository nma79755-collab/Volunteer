package com.example.registrationservice.Service;

import com.cyh.entity.Activity;
import com.cyh.entity.Registration;
import com.cyh.entity.RegistrationStatus;
import com.cyh.enums.ActivityStatus;
import com.cyh.exception.BusinessException;
import com.example.registrationservice.mapper.RegistrationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Registration Validator
 *
 * Validates registration submission and cancellation operations.
 */
@Slf4j
@Component
public class RegistrationValidator {
@Autowired
private RegistrationMapper registrationMapper;
    /**
     * Validate registration submission
     *
     * @param activity         the activity to register for
     * @param existingRegistration existing registration record (null if none)
     * @throws BusinessException if validation fails
     */
    public void validateRegistrationCreate(Activity activity, Registration existingRegistration) {
        log.debug("Validating registration creation for activity: {}", activity.getId());

        // Check activity exists and is not deleted
        if (activity.getIsDeleted() != null && activity.getIsDeleted() == 1) {
            log.warn("Activity is deleted: {}", activity.getId());
            throw new BusinessException("活动不存在");
        }

        // Check activity status - only PUBLISHED or IN_PROGRESS activities accept registrations
        int status = activity.getStatus();
        if (status != ActivityStatus.PUBLISHED.getCode() && status != ActivityStatus.IN_PROGRESS.getCode()) {
            log.warn("Activity is not accepting registrations, status: {}", status);
            throw new BusinessException("活动当前不接受报名");
        }

        // Check for duplicate registration
        if (existingRegistration != null) {
            int regStatus = existingRegistration.getStatus();
            // Only block if not cancelled/rejected
            if (regStatus != RegistrationStatus.CANCELLED.getCode()
                    && regStatus != RegistrationStatus.REJECTED.getCode()) {
                log.warn("User already registered for activity: {}", activity.getId());
                throw new BusinessException("您已报名该活动");
            }
        }
        // Check capacity
        if (activity.getRegisteredCount() >= activity.getRequiredCount()) {
            log.warn("Activity is full: {}", activity.getId());
            throw new BusinessException("活动已满员");
        }
    }

    /**
     * Validate registration cancellation
     *
     * @param registration the registration to cancel
     * @param currentUserId the ID of the user requesting cancellation
     * @throws BusinessException if validation fails
     */
    public void validateRegistrationCancel(Registration registration, Long currentUserId) {
        log.debug("Validating registration cancellation for registration: {}", registration.getId());

        // Check ownership
        if (!registration.getUserId().equals(currentUserId)) {
            log.warn("User {} is not the owner of registration {}", currentUserId, registration.getId());
            throw new BusinessException("无权取消该报名");
        }

        // Only PENDING_REVIEW or APPROVED can be cancelled
        int status = registration.getStatus();
        if (status != RegistrationStatus.PENDING_REVIEW.getCode()
                && status != RegistrationStatus.APPROVED.getCode()) {
            log.warn("Cannot cancel registration with status: {}", status);
            throw new BusinessException("只能取消待审核或已批准状态的报名");
        }
    }
}
