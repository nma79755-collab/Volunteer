package com.cyh.reviewservice.Service;


import com.cyh.entity.Activity;
import com.cyh.entity.Registration;
import com.cyh.entity.RegistrationStatus;
import com.cyh.exception.BusinessException;
import com.cyh.reviewservice.Dto.ReviewBatchRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Review Validator
 *
 * Validates approve and reject operations for registration reviews.
 */
@Slf4j
@Component
public class ReviewValidator {

    /**
     * Validate that a registration can be approved
     *
     * @param registration the registration to approve
     * @param activity     the associated activity
     * @throws BusinessException if validation fails
     */
    public void validateApprove(Registration registration, Activity activity) {
        log.debug("Validating approve for registration: {}", registration.getId());

        if (registration.getIsDeleted() != null && registration.getIsDeleted() == 1) {
            throw new BusinessException("报名记录不存在");
        }

        // Only PENDING_REVIEW registrations can be approved
        if (registration.getStatus() != RegistrationStatus.PENDING_REVIEW.getCode()) {
            log.warn("Registration {} is not in PENDING_REVIEW status: {}", registration.getId(), registration.getStatus());
            throw new BusinessException("只能审核待审核状态的报名");
        }

        // Check activity capacity
        if (activity == null || activity.getIsDeleted() == 1) {
            throw new BusinessException("活动不存在");
        }

        if (activity.getRegisteredCount() >= activity.getRequiredCount()) {
            log.warn("Activity {} is full: {}/{}", activity.getId(), activity.getRegisteredCount(), activity.getRequiredCount());
            throw new BusinessException("活动已满员，无法批准");
        }
    }

    /**
     * Validate that a registration can be rejected
     *
     * @param registration the registration to reject
     * @param reason       the rejection reason
     * @throws BusinessException if validation fails
     */
    public void validateReject(Registration registration, String reason) {
        log.debug("Validating reject for registration: {}", registration.getId());

        if (registration.getIsDeleted() != null && registration.getIsDeleted() == 1) {
            throw new BusinessException("报名记录不存在");
        }

        // Only PENDING_REVIEW registrations can be rejected
        if (registration.getStatus() != RegistrationStatus.PENDING_REVIEW.getCode()) {
            log.warn("Registration {} is not in PENDING_REVIEW status: {}", registration.getId(), registration.getStatus());
            throw new BusinessException("只能审核待审核状态的报名");
        }

        // Rejection reason is required
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException("拒绝原因不能为空");
        }
    }

    /**
     * Validate a batch review request
     *
     * @param request the batch review request
     * @throws BusinessException if validation fails
     */
    public void validateBatchRequest(ReviewBatchRequest request) {
        log.debug("Validating batch review request with {} IDs", request.getRegistrationIds().size());

        if (request.getRegistrationIds() == null || request.getRegistrationIds().isEmpty()) {
            throw new BusinessException("报名ID列表不能为空");
        }

        if ("reject".equals(request.getAction()) && !StringUtils.hasText(request.getReason())) {
            throw new BusinessException("批量拒绝时必须提供拒绝原因");
        }
    }
}
