package com.cyh.activityservice.Service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cyh.Client.RegistrationClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyh.Dto.GeocodeResponse;
import com.cyh.Dto.LocationDto;
import com.cyh.Utils.HttpClientUtil;
import com.cyh.Utils.JwtUserDetails;
import com.cyh.Utils.UserContext;
import com.cyh.activityservice.Aspect.OperationLogAnnotation;
import com.cyh.activityservice.Dto.ActivityCreateRequest;
import com.cyh.activityservice.Dto.ActivityUpdateRequest;
import com.cyh.activityservice.enums.ActivityStatus;
import com.cyh.activityservice.mapper.ActivityMapper;
import com.cyh.entity.*;
import com.cyh.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Activity Service
 * 
 * Handles business logic for activity management including creation, publishing,
 * updating, deletion, and querying.
 */
@Slf4j
@Service
public class ActivityService {
    
    @Autowired
    private ActivityMapper activityMapper;
    
    @Autowired
    private RegistrationClient registrationClient;
    
    @Autowired
    private ActivityValidator activityValidator;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    /**
     * Create a new activity
     * 
     * Validates the activity creation request, creates a new activity entity,
     * and saves it to the database with DRAFT status.
     * 
     * @param request the activity creation request containing activity details
     * @return the created activity with assigned ID
     * @throws BusinessException if validation fails
     */
    @Transactional
    @OperationLogAnnotation(type = "CREATE", object = "ACTIVITY")
    public Activity createActivity(ActivityCreateRequest request) {
        log.info("Creating new activity: {}", request.getName());
        
        // Validate request
        activityValidator.validateActivityCreate(request);
        
        // Get current user ID
        Long userId = getCurrentUserId();

        // Non-admin users cannot set basePoints
        int basePoints = isCurrentUserAdmin() && request.getBasePoints() != null
                ? request.getBasePoints() : 0;
        
        // Create activity entity
        Activity activity = Activity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .location(request.getLocation())
                .requiredCount(request.getRequiredCount())
                .registeredCount(0)
                .basePoints(basePoints)
                .status(ActivityStatus.DRAFT.getCode())
                .createdBy(userId)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDeleted(0)
                .build();
        HashMap<String, String> map = new HashMap<>();
        map.put("address",activity.getLocation());
        map.put("key","c060cf3dc2a148e9e8eba034a12efc57");
        String s = HttpClientUtil.doGet("https://restapi.amap.com/v3/geocode/geo", map);
        JSONObject entries = JSONUtil.parseObj(s);
        GeocodeResponse dto = entries.toBean(GeocodeResponse.class);
        String location = dto.getGeocodes().get(0).getLocation();
        activity.setPosition(location);
        // Save to database
        activityMapper.insert(activity);
        activityMapper.insertImag(activity.getId(),request.getImageUrls());
        log.info("Activity created successfully with ID: {}", activity.getId());
        
        return activity;
    }
    
    /**
     * Approve an activity (admin only) — sets approved=1 and optionally sets basePoints
     */
    public Activity approveActivity(Long activityId, Integer basePoints) {
        log.info("Approving activity: {}", activityId);
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || activity.getIsDeleted() == 1) {
            throw new BusinessException("活动不存在");
        }
        activity.setApproved(1);
        if (basePoints != null) {
            activity.setBasePoints(basePoints);
        }
        activity.setUpdateTime(LocalDateTime.now());
        activityMapper.updateById(activity);
        Message message = new Message();
        message.setReceiver(String.valueOf(activity.getCreatedBy()));
        message.setContent("您的活动创建申请通过"+";活动名:"+activity.getName());
        message.setSender(UserContext.getUser());
        message.setSendTime(LocalDateTime.now());
        rabbitTemplate.convertAndSend("directExchangeForActivity", "approveAct", message);
        log.info("Activity {} approved", activityId);
        return activity;
    }

    /**
     * Publish an activity (change status from DRAFT to PUBLISHED)
     * 
     * Validates that the activity is in DRAFT status, then transitions it to PUBLISHED
     * status, making it visible to volunteers for registration.
     * 
     * @param activityId the activity ID to publish
     * @return the published activity with updated status
     * @throws BusinessException if activity not found or not in DRAFT status
     */
    @Transactional
    @OperationLogAnnotation(type = "PUBLISH", object = "ACTIVITY")
    public Activity publishActivity(Long activityId) {
        log.info("Publishing activity: {}", activityId);
        
        // Get activity
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            log.warn("Activity not found: {}", activityId);
            throw new BusinessException("活动不存在");
        }
        
        // Validate publish
        activityValidator.validateActivityPublish(activity);
        
        // Check ownership
        assertOwnerOrAdmin(activity);

        // Non-admin created activities must be approved by admin before publishing
        if (!isCurrentUserAdmin() && (activity.getApproved() == null || activity.getApproved() != 1)) {
            throw new BusinessException("活动尚未通过管理员审核，无法发布");
        }
        
        // Update status
        activity.setStatus(ActivityStatus.PUBLISHED.getCode());
        activity.setUpdateTime(LocalDateTime.now());
        activityMapper.updateById(activity);
        
        log.info("Activity published successfully: {}", activityId);
        return activity;
    }
    
    /**
     * Update an activity (only DRAFT status can be updated)
     * 
     * Validates that the activity is in DRAFT status, then updates its fields
     * with the provided information.
     * 
     * @param activityId the activity ID to update
     * @param request the activity update request containing new details
     * @return the updated activity
     * @throws BusinessException if activity not found or not in DRAFT status
     */
    @Transactional
    @OperationLogAnnotation(type = "UPDATE", object = "ACTIVITY")
    public Activity updateActivity(Long activityId, ActivityUpdateRequest request) {
        log.info("Updating activity: {}", activityId);

        // Get activity
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            log.warn("Activity not found: {}", activityId);
            throw new BusinessException("活动不存在");
        }

        // Validate update
        activityValidator.validateActivityUpdate(activity, request);

        // Check ownership
        assertOwnerOrAdmin(activity);

        // Update fields
        activity.setName(request.getName());
        activity.setDescription(request.getDescription());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setLocation(request.getLocation());
        activity.setRequiredCount(request.getRequiredCount());
        if (isCurrentUserAdmin() && request.getBasePoints() != null) {
            activity.setBasePoints(request.getBasePoints());
        }
        activity.setUpdateTime(LocalDateTime.now());
        HashMap<String, String> map = new HashMap<>();
        map.put("address",activity.getLocation());
        map.put("key","c060cf3dc2a148e9e8eba034a12efc57");
        String s = HttpClientUtil.doGet("https://restapi.amap.com/v3/geocode/geo", map);
        JSONObject entries = JSONUtil.parseObj(s);
        GeocodeResponse dto = entries.toBean(GeocodeResponse.class);
        String location = dto.getGeocodes().get(0).getLocation();
        activity.setPosition(location);
        // Save to database
        activityMapper.updateById(activity);

        // 更新图片
        if (request.getImageUrls() != null) {
            activityMapper.deleteImg(activityId);
            if (!request.getImageUrls().isEmpty()) {
                activityMapper.insertImag(activityId, request.getImageUrls());
            }
        }

        log.info("Activity updated successfully: {}", activityId);
        return activity;
    }
    /**
     * Cancel an activity (owner or admin only)
     * 
     * Changes the activity status from PUBLISHED to CANCELED
     * 
     * @param activityId the activity ID to cancel
     * @throws BusinessException if activity not found or user has no permission
     */
    @Transactional
    @OperationLogAnnotation(type = "CANCEL", object = "ACTIVITY")
    public void cancelActivity(Long activityId) {
        log.info("Canceling activity: {}", activityId);
        
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || activity.getIsDeleted() == 1) {
            log.warn("Activity not found: {}", activityId);
            throw new BusinessException("活动不存在");
        }

        assertOwnerOrAdmin(activity);
        
        activity.setStatus(0);
        activity.setUpdateTime(LocalDateTime.now());
        activity.setApproved(0);
        activityMapper.updateById(activity);
        
        log.info("Activity canceled successfully: {}", activityId);
    }

    public Activity rejectActivity(Long activityId) {
        log.info("Rejecting activity: {}", activityId);
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || activity.getIsDeleted() == 1) {
            throw new BusinessException("活动不存在");
        }
        activity.setApproved(2);
        activity.setUpdateTime(LocalDateTime.now());
        activityMapper.updateById(activity);
        
        log.info("Activity {} rejected", activityId);
        
        Message message = new Message();
        message.setReceiver(String.valueOf(activity.getCreatedBy()));
        message.setContent("您的活动创建申请已被拒绝，活动名:" + activity.getName());
        message.setSender(UserContext.getUser());
        message.setSendTime(LocalDateTime.now());
        rabbitTemplate.convertAndSend("directExchangeForActivity", "approveAct", message);
        
        return activity;
    }
    
    /**
     * Delete an activity (check for approved registrations)
     * 
     * Performs logical deletion of an activity. Checks if there are any approved
     * registrations for the activity. If approved registrations exist, deletion is
     * rejected to maintain data integrity.
     * 
     * @param activityId the activity ID to delete
     * @throws BusinessException if activity not found or has approved registrations
     */
    @Transactional
    @OperationLogAnnotation(type = "DELETE", object = "ACTIVITY")
    public void deleteActivity(Long activityId) {
        log.info("Deleting activity: {}", activityId);
        
        // Get activity
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            log.warn("Activity not found: {}", activityId);
            throw new BusinessException("活动不存在");
        }
        
        // Check for approved registrations
        assertOwnerOrAdmin(activity);
        
        long approvedCount = registrationClient.getCount(activityId,RegistrationStatus.APPROVED.getCode(),0);
        if (approvedCount > 0) {
            log.warn("Cannot delete activity with approved registrations: {}", activityId);
            throw new BusinessException("存在已批准的报名记录，无法删除");
        }
        
        // Perform logical delete
        activity.setIsDeleted(1);
        activity.setUpdateTime(LocalDateTime.now());
        activityMapper.update(new UpdateWrapper<Activity>().eq("id",activity.getId()).set("update_time",activity.getUpdateTime()).set("is_deleted",1));
        
        log.info("活动成功删除: {}", activityId);
    }
    
    /**
     * Get activity list with pagination and filtering
     * 
     * Retrieves a paginated list of activities with optional filtering by status
     * and keyword search. Results are ordered by creation time in descending order.
     * 
     * @param pageNum the page number (1-indexed)
     * @param pageSize the number of records per page
     * @param status the activity status filter (optional, null for all statuses)
     * @param keyword the search keyword for name and description (optional)
     * @return a page of activities matching the criteria
     */
    public Page<Activity> getActivityList(int pageNum, int pageSize, Integer status, String keyword) {
        log.info("Querying activity list - page: {}, size: {}, status: {}, keyword: {}", 
                pageNum, pageSize, status, keyword);
        
        QueryWrapper<Activity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_deleted", 0);

        // Non-admin users can only see non-DRAFT activities or their own drafts
        if (!isCurrentUserAdmin()) {
            Long currentUserId = null;
            try { currentUserId = getCurrentUserId(); } catch (Exception ignored) {}
            if (currentUserId != null) {
                final Long uid = currentUserId;
                queryWrapper.and(q -> q
                    .ne("status", ActivityStatus.DRAFT.getCode())
                    .or()
                    .eq("created_by", uid)
                );
            } else {
                queryWrapper.ne("status", ActivityStatus.DRAFT.getCode());
            }
        }
        
        // Filter by status if provided
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        
        // Filter by keyword if provided
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(q -> q.like("name", keyword).or().like("description", keyword));
        }
        
        // Order by creation time descending
        queryWrapper.orderByDesc("create_time");

        // Execute query
        Page<Activity> page = new Page<>(pageNum, pageSize);
        Page<Activity> activityPage = activityMapper.selectPage(page, queryWrapper);
        List<Long> list = activityPage.getRecords().stream().map(Activity::getId).toList();//获取所有活动的id
        List<ActivityImg> imgs = activityMapper.getImg(list);
        Map<Long, List<String>> imageMap = new HashMap<>();
        for (ActivityImg img : imgs) {
            if (img.getUrl() == null || img.getUrl().isEmpty()) continue;
            imageMap.computeIfAbsent(img.getActivityId(), k -> new ArrayList<>())
                    .add(img.getUrl());
        }

        page.getRecords().forEach(activity -> {
            activity.setImageUrl(imageMap.getOrDefault(activity.getId(), Collections.emptyList()));
        });
        return page;
    }
    /**
     * Get activity detail
     * 
     * Retrieves the complete details of a specific activity by its ID.
     * Returns the activity if it exists and is not deleted.
     * 
     * @param activityId the activity ID
     * @return the activity details
     * @throws BusinessException if activity not found or is deleted
     */
    public Activity getActivityDetail(Long activityId) {
        log.info("Getting activity detail: {}", activityId);
        
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || activity.getIsDeleted() == 1) {
            log.warn("Activity not found: {}", activityId);
            throw new BusinessException("活动不存在");
        }

        List<ActivityImg> imgs = activityMapper.getImg(List.of(activityId));
        List<String> imageUrls = new ArrayList<>();
        for (ActivityImg img : imgs) {
            if (img.getUrl() != null && !img.getUrl().isEmpty()) {
                imageUrls.add(img.getUrl());
            }
        }
        activity.setImageUrl(imageUrls);

        return activity;
    }
    
    /**
     * Get current user ID from security context
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof JwtUserDetails) {
            return ((JwtUserDetails) authentication.getDetails()).getUserId();
        }
        throw new BusinessException("用户未认证");
    }

    /**
     * Check if current user is ADMIN
     */
    private boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof JwtUserDetails) {
            return "ADMIN".equals(((JwtUserDetails) authentication.getDetails()).getRole());
        }
        return false;
    }

    /**
     * Assert current user owns the activity or is ADMIN
     */
    private void assertOwnerOrAdmin(Activity activity) {
        if (isCurrentUserAdmin()) return;
        Long userId = getCurrentUserId();
        if (!userId.equals(activity.getCreatedBy())) {
            throw new BusinessException("无权操作他人的活动");
        }
    }
}
