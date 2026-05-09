package com.cyh.reviewservice.Service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyh.Client.ActivityClient;
import com.cyh.Client.RegistrationClient;
import com.cyh.Dto.ActivityUpdateRequest;
import com.cyh.Utils.JwtUserDetails;
import com.cyh.Utils.UserContext;
import com.cyh.entity.Activity;
import com.cyh.entity.Message;
import com.cyh.entity.Registration;
import com.cyh.entity.RegistrationStatus;
import com.cyh.exception.BusinessException;
import com.cyh.reviewservice.Dto.ReviewBatchRequest;
import com.cyh.reviewservice.Dto.ReviewBatchResponse;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Review Service
 *
 * Handles business logic for admin review of volunteer registrations,
 * including approve, reject, and batch operations.
 */
@Slf4j
@Service
public class ReviewService {

    @Autowired
    private RegistrationClient registrationClient;

    @Autowired
    private ActivityClient activityClient;

    @Autowired
    private ReviewValidator reviewValidator;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * Approve a registration
     *
     * @param registrationId the registration ID to approve
     * @return the updated registration
     * @throws BusinessException if validation fails
     */
    public Registration approveRegistration(Long registrationId) {
        Long reviewerId = getCurrentUserId();
        log.info("Admin {} approving registration {}", reviewerId, registrationId);

        Registration registration = getRegistrationOrThrow(registrationId);

        Long userId = registration.getUserId();//获取报名的用户id
        Activity activity = getActivityOrThrow(registration.getActivityId());

        assertActivityOwnerOrAdmin(activity);
        reviewValidator.validateApprove(registration, activity);
        registration.setStatus(RegistrationStatus.APPROVED.getCode());
        registration.setReviewedBy(reviewerId);
        registration.setReviewTime(LocalDateTime.now());
        registration.setUpdateTime(LocalDateTime.now());
        registrationClient.updateRegistration(registration);
        activityClient.updateAcById(activity.getId(), activity.getRegisteredCount()+1);
        // Close registrations if activity is now full
        if (isActivityFull(activity)) {
            log.info("Activity {} is now full, closing registrations", activity.getId());
        }

        log.info("Registration {} approved successfully", registrationId);
        Message message = new Message();
        message.setReceiver(String.valueOf(userId));
        message.setSender(UserContext.getUser());
        message.setSendTime(LocalDateTime.now());
        message.setContent("您的报名申请已被同意,审核员ID为"+UserContext.getUser()+";活动名:"+activity.getName());
        rabbitTemplate.convertAndSend("directExchangeForReview","review",message);
        return registration;
    }

    /**
     * Reject a registration
     *
     * @param registrationId the registration ID to reject
     * @param reason         the rejection reason
     * @return the updated registration
     * @throws BusinessException if validation fails
     */
    public Registration rejectRegistration(Long registrationId, String reason) {
        Long reviewerId = getCurrentUserId();
        log.info("Admin {} rejecting registration {}", reviewerId, registrationId);

        Registration registration = getRegistrationOrThrow(registrationId);

        reviewValidator.validateReject(registration, reason);

        // Decrement registered count since this registration is being rejected
        Activity activity = activityClient.getActivityDetail(registration.getActivityId()).getData();
        if (activity != null) {
            assertActivityOwnerOrAdmin(activity);
            if (activity.getRegisteredCount() > 0) {
                activity.setRegisteredCount(activity.getRegisteredCount() - 1);
                activity.setUpdateTime(LocalDateTime.now());
                activityClient.updateActivityById(activity);
            }
        }

        registration.setStatus(RegistrationStatus.REJECTED.getCode());
        registration.setRejectReason(reason);
        registration.setReviewedBy(reviewerId);
        registration.setReviewTime(LocalDateTime.now());
        registration.setUpdateTime(LocalDateTime.now());
        registrationClient.updateRegistration(registration);

        log.info("Registration {} rejected successfully", registrationId);
        Long userId = registration.getUserId();
        Message message = new Message();
        message.setReceiver(String.valueOf(userId));
        message.setSender(UserContext.getUser());
        message.setSendTime(LocalDateTime.now());
        if (activity != null) {
            message.setContent("您的报名申请已被拒绝,审核员ID为"+UserContext.getUser()+";活动名:"+activity.getName()+";拒绝理由为"+ reason);
        } else {
            message.setContent("您的报名申请已被拒绝,审核员ID为"+UserContext.getUser()+";拒绝理由为"+ reason);
        }
        rabbitTemplate.convertAndSend("directExchangeForReview","review",message);
        return registration;
    }

    /**
     * Get paginated list of pending registrations.
     * ADMIN sees all; activity owners see only their own activities' pending registrations.
     */
    public Page<Registration> getPendingRegistrations(int pageNum, int pageSize) {
        log.info("Getting pending registrations - page: {}, size: {}", pageNum, pageSize);



        // Non-admin: only show pending registrations for activities they created
        List<Long> myActivityIds = null;
        Long currentUserId = getCurrentUserId();
            // Get activity IDs created by this user
            myActivityIds = activityClient.getActivitylistByCreat(currentUserId, 0)
                    .stream().map(Activity::getId).collect(java.util.stream.Collectors.toList());
        System.out.println("===== myActivityIds size: " + myActivityIds.size());
        System.out.println("===== myActivityIds: " + myActivityIds);
            if (myActivityIds.isEmpty()) {
                return new Page<>(pageNum, pageSize); // empty result
            }
        List<Registration> registration = registrationClient.getRegistration(myActivityIds);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Registration> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        page.setRecords(registration);
        page.setTotal(registration.size());

        return page;
    }

    /**
     * Batch approve or reject registrations
     *
     * @param request the batch review request
     * @return batch result with success and failure counts
     */
    @Transactional
    public ReviewBatchResponse batchReview(ReviewBatchRequest request) {
        log.info("Batch review: action={}, count={}", request.getAction(), request.getRegistrationIds().size());

        reviewValidator.validateBatchRequest(request);

        int successCount = 0;
        int failureCount = 0;

        for (Long registrationId : request.getRegistrationIds()) {
            try {
                if ("APPROVE".equals(request.getAction())) {
                    approveRegistration(registrationId);
                } else {
                    rejectRegistration(registrationId, request.getReason());
                }
                successCount++;
            } catch (BusinessException e) {
                log.warn("Batch review failed for registration {}: {}", registrationId, e.getMessage());
                failureCount++;
            }
        }

        log.info("Batch review completed: success={}, failure={}", successCount, failureCount);
        return ReviewBatchResponse.builder()
                .successCount(successCount)
                .failureCount(failureCount)
                .build();
    }

    /**
     * Check if an activity has reached its capacity
     *
     * @param activity the activity to check
     * @return true if the activity is full
     */
    public boolean isActivityFull(Activity activity) {
        return activity.getRegisteredCount() >= activity.getRequiredCount();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Registration getRegistrationOrThrow(Long registrationId) {
        Registration registration = registrationClient.getRegistrationDetail(registrationId).getBody().getData();
        System.out.println("获取到的报名记录为"+ registration);
        if (registration == null || registration.getIsDeleted() == 1) {
            log.warn("Registration not found: {}", registrationId);
            throw new BusinessException("报名记录不存在");
        }
        return registration;
    }

    private Activity getActivityOrThrow(Long activityId) {
        Activity activity = activityClient.getActivityDetail(activityId).getData();
        if (activity == null || Integer.valueOf(1).equals(activity.getIsDeleted())) {
            log.warn("Activity not found: {}", activityId);
            throw new BusinessException("活动不存在");
        }
        return activity;
    }

    /**
     * Get current user ID from security context
     *
     * @return the current user ID
     * @throws BusinessException if user is not authenticated
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("用户未认证");
        }

        Object details = authentication.getDetails();
        if (details instanceof JwtUserDetails) {
            return ((JwtUserDetails) details).getUserId();
        }

        try {
            return Long.parseLong(authentication.getPrincipal().toString());
        } catch (Exception e) {
            log.error("Failed to extract user ID from authentication", e);
            throw new BusinessException("无法获取当前用户信息");
        }
    }

    private boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof JwtUserDetails) {
            return "ADMIN".equals(((JwtUserDetails) authentication.getDetails()).getRole());
        }
        return false;
    }

    private void assertActivityOwnerOrAdmin(Activity activity) {
        if (isCurrentUserAdmin()) return;
        Long userId = getCurrentUserId();
        if (!userId.equals(activity.getCreatedBy())) {
            throw new BusinessException("无权审核他人活动的报名");
        }
    }
}
