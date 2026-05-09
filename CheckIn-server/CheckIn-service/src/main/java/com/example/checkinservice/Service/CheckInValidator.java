package com.example.checkinservice.Service;

import com.cyh.entity.Activity;
import com.cyh.entity.CheckIn;
import com.cyh.entity.Registration;
import com.cyh.entity.RegistrationStatus;
import com.cyh.enums.ActivityStatus;
import com.cyh.exception.BusinessException;
import com.example.checkinservice.enums.CheckInStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Check-in Validator
 *
 * Validates check-in operations to ensure business rules are enforced.
 * Requirements: 4, 7
 */
@Slf4j
@Component
public class CheckInValidator {

    /**
     * Validate that a check-in can be performed.
     *
     * Rules:
     * - Registration must exist and be APPROVED
     * - Volunteer must not have already checked in
     * - Activity must be IN_PROGRESS
     *
     * @param registration the registration record
     * @param activity     the associated activity
     * @param existing     existing check-in record (may be null)
     * @throws BusinessException if any validation rule is violated
     */
    public void validateCheckIn(Registration registration, Activity activity, CheckIn existing) {
        log.debug("Validating check-in for registration: {}", registration.getId());

        // Registration must be approved
        if (registration.getStatus() != RegistrationStatus.APPROVED.getCode()) {
            log.warn("Registration {} is not APPROVED, status: {}", registration.getId(), registration.getStatus());
            throw new BusinessException("只有已批准的报名才能签到");
        }

        // Must not have already checked in (status != NOT_CHECKED_IN means already processed)
        if (existing != null && existing.getIsDeleted() != 1) {
            int existingStatus = existing.getStatus();
            if (existingStatus == CheckInStatus.CHECKED_IN.getCode()
                    || existingStatus == CheckInStatus.LATE.getCode()) {
                log.warn("Registration {} has already checked in", registration.getId());
                throw new BusinessException("该报名已签到，不能重复签到");
            }
            if (existingStatus == CheckInStatus.ABSENT.getCode()) {
                log.warn("Registration {} is marked absent", registration.getId());
                throw new BusinessException("该报名已被标记为缺席，无法签到");
            }
        }

        // Activity must be IN_PROGRESS
        if (activity.getStatus() != ActivityStatus.IN_PROGRESS.getCode()) {
            log.warn("Activity {} is not IN_PROGRESS, status: {}", activity.getId(), activity.getStatus());
            throw new BusinessException("活动未进行中，无法签到");
        }
    }
}
