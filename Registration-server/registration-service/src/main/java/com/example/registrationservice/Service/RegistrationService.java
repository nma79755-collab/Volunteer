package com.example.registrationservice.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyh.Client.ActivityClient;
import com.cyh.Utils.JwtUserDetails;
import com.cyh.Utils.UserContext;
import com.cyh.entity.Activity;
import com.cyh.entity.Message;
import com.cyh.entity.Registration;
import com.cyh.entity.RegistrationStatus;
import com.cyh.exception.BusinessException;
import com.example.registrationservice.Aspect.OperationLogAnnotation;
import com.example.registrationservice.Dto.RegistrationCreateRequest;
import com.example.registrationservice.mapper.RegistrationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Registration Service
 *
 * Handles business logic for volunteer registration management including
 * submission, cancellation, and querying.
 */
@Slf4j
@Service
public class RegistrationService {

    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private ActivityClient activityClient;

    @Autowired
    private RegistrationValidator registrationValidator;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * Submit a registration for an activity
     *
     * @param request the registration creation request
     * @return the created registration
     * @throws BusinessException if validation fails
     */
    @OperationLogAnnotation(type = "CREATE", object = "REGISTRATION")
    public Registration submitRegistration(RegistrationCreateRequest request) {
        Long userId = getCurrentUserId();
        Long activityId = request.getActivityId();
        log.info("User {} submitting registration for activity {}", userId, activityId);

        Activity activity = activityClient.getActivityDetail(activityId).getData();

        Map<String, Long> params = new HashMap<>();
        params.put("userId", userId);
        params.put("activityId", activityId);
        Registration existing = registrationMapper.findByUserAndActivity(params);

        if (userId.equals(activity.getCreatedBy())) {
            throw new BusinessException("不能报名自己创建的活动");
        }

        registrationValidator.validateRegistrationCreate(activity, existing);

        if (existing != null) {
            existing.setStatus(RegistrationStatus.PENDING_REVIEW.getCode());
            existing.setUpdateTime(LocalDateTime.now());
            registrationMapper.updateById(existing);
            log.info("Registration reused with ID: {}", existing.getId());
            return existing;
        }

        // 首次报名，插入新记录
        Registration registration = Registration.builder()
                .userId(userId)
                .activityId(activityId)
                .status(RegistrationStatus.PENDING_REVIEW.getCode())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDeleted(0)
                .build();

        registrationMapper.insert(registration);

        activity.setUpdateTime(LocalDateTime.now());
        activityClient.updateActivityById(activity);
        Message message = new Message();
        message.setReceiver(String.valueOf(activity.getCreatedBy()));
        message.setSender(UserContext.getUser());
        message.setSendTime(LocalDateTime.now());
        message.setContent("用户" + UserContext.getUser() + "报名了活动" + activity.getName());
        rabbitTemplate.convertAndSend("directExchangeForActivity", "approveAct", message);
        log.info("Registration created successfully with ID: {}", registration.getId());
        return registration;
    }

    /**
     * Cancel a registration
     *
     * @param registrationId the registration ID to cancel
     * @throws BusinessException if validation fails
     */
    @Transactional
    @OperationLogAnnotation(type = "CANCEL", object = "REGISTRATION")
    public void cancelRegistration(Long registrationId) {
        Long userId = getCurrentUserId();
        log.info("User {} cancelling registration {}", userId, registrationId);

        // Get registration
        Registration registration = registrationMapper.selectById(registrationId);
        if (registration == null || registration.getIsDeleted() == 1) {
            log.warn("Registration not found: {}", registrationId);
            throw new BusinessException("报名记录不存在");
        }

        // Validate cancellation
        registrationValidator.validateRegistrationCancel(registration, userId);

        // Capture original status before updating
        boolean wasApproved = registration.getStatus() == RegistrationStatus.APPROVED.getCode();

        // Update status to CANCELLED
        registration.setStatus(RegistrationStatus.CANCELLED.getCode());
        registration.setUpdateTime(LocalDateTime.now());
        registrationMapper.updateById(registration);

        // Decrement registered count on activity if was APPROVED
        if (wasApproved) {
            Activity activity = activityClient.getActivityDetail(registration.getActivityId()).getData();
            if (activity != null && activity.getRegisteredCount() > 0) {
                activity.setRegisteredCount(activity.getRegisteredCount() - 1);
                activity.setUpdateTime(LocalDateTime.now());
                activityClient.updateActivityById(activity);
            }
        }

        log.info("Registration {} cancelled successfully", registrationId);
    }

    /**
     * Get personal registration list with pagination
     *
     * @param pageNum  the page number
     * @param pageSize the page size
     * @return paginated registration records
     */
    public Page<Registration> getMyRegistrations(int pageNum, int pageSize) {
        Long userId = getCurrentUserId();
        log.info("Getting registrations for user {} - page: {}, size: {}", userId, pageNum, pageSize);

        QueryWrapper<Registration> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .eq("is_deleted", 0)
                .orderByDesc("create_time");

        Page<Registration> page = new Page<>(pageNum, pageSize);
        return registrationMapper.selectPage(page, queryWrapper);
    }

    /**
     * Get registration detail by ID
     *
     * @param registrationId the registration ID
     * @return the registration
     * @throws BusinessException if not found or not owned by current user
     */
    public Registration getRegistrationDetail(Long registrationId) {
        Long userId = getCurrentUserId();
        log.info("Getting registration detail {} for user {}", registrationId, userId);

        Registration registration = registrationMapper.selectById(registrationId);
        if (registration == null || registration.getIsDeleted() == 1) {
            log.warn("Registration not found: {}", registrationId);
            throw new BusinessException("报名记录不存在");
        }
        // Volunteers can only view their own registrations; admins can view all
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return registration;
    }

    /**
     * Check if current user has an active registration for an activity
     *
     * @param activityId the activity ID
     * @return the existing registration, or null if not registered
     */
    public Registration checkRegistration(Long activityId) {
        Long userId = getCurrentUserId();
        Map<String, Long> params = new HashMap<>();
        params.put("userId", userId);
        params.put("activityId", activityId);
        Registration existing = registrationMapper.findByUserAndActivity(params);
        if (existing != null && existing.getStatus() != RegistrationStatus.CANCELLED.getCode()) {
            return existing;
        }
        return null;
    }

    /**
     * Update registration status (for admin use)
     *
     * @param registrationId the registration ID
     * @param status         the new status
     * @param rejectReason   the reject reason (optional)
     * @return the updated registration
     */
    @Transactional
    public Registration updateRegistrationStatus(Long registrationId, Integer status, String rejectReason) {
        log.info("Updating registration {} status to {}", registrationId, status);

        Registration registration = registrationMapper.selectById(registrationId);
        if (registration == null || registration.getIsDeleted() == 1) {
            log.warn("Registration not found: {}", registrationId);
            throw new BusinessException("报名记录不存在");
        }

        Long reviewerId = getCurrentUserId();
        registration.setStatus(status);
        registration.setRejectReason(rejectReason);
        registration.setReviewedBy(reviewerId);
        registration.setReviewTime(LocalDateTime.now());
        registration.setUpdateTime(LocalDateTime.now());
        registrationMapper.updateById(registration);

        log.info("Registration {} status updated to {}", registrationId, status);
        return registration;
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

        // JwtUserDetails is stored in authentication details
        Object details = authentication.getDetails();
        if (details instanceof JwtUserDetails) {
            return ((JwtUserDetails) details).getUserId();
        }

        // Fallback: try parsing principal as user ID
        try {
            return Long.parseLong(authentication.getPrincipal().toString());
        } catch (Exception e) {
            log.error("Failed to extract user ID from authentication", e);
            throw new BusinessException("无法获取当前用户信息");
        }
    }
}
