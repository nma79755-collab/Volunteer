package com.cyh.activityservice.Controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyh.Dto.DistanceDto;
import com.cyh.Dto.LocationDto;
import com.cyh.Res.ApiResponse;
import com.cyh.Utils.HttpClientUtil;
import com.cyh.Utils.UserContext;
import com.cyh.activityservice.Dto.ActivityCreateRequest;
import com.cyh.activityservice.Dto.ActivityListResponse;
import com.cyh.activityservice.Dto.ActivityResponse;
import com.cyh.activityservice.Dto.ActivityUpdateRequest;
import com.cyh.activityservice.Service.ActivityService;
import com.cyh.activityservice.mapper.ActivityMapper;
import com.cyh.entity.Activity;
import com.cyh.entity.ActivityImg;
import com.cyh.entity.Message;
import com.cyh.entity.User;
import com.cyh.enums.ActivityStatus;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Activity Controller
 * 
 * Handles REST endpoints for activity management including creation, publishing,
 * updating, deletion, and querying.
 */
@Slf4j
@RestController
@RequestMapping("/api/activities")
public class ActivityController {
    
    @Autowired
    private ActivityService activityService;
    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    /**
     * Create a new activity (any authenticated user)
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> createActivity(@Valid @RequestBody ActivityCreateRequest request) {
        log.info("Create activity endpoint called with name: {}", request.getName());
        
        Activity activity = activityService.createActivity(request);
        ActivityResponse response = mapToResponse(activity);
        Message message = new Message();
        message.setContent("有新的活动创建请求");
        message.setSender(UserContext.getUser());
        message.setSendTime(LocalDateTime.now());
        rabbitTemplate.convertAndSend("directExchangeForActivity", "createAct", message);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("活动创建成功", response));
    }
    
    /**
     * Approve an activity (admin only)
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> approveActivity(
            @PathVariable Long id,
            @RequestParam(required = false) Integer basePoints) {
        log.info("Approve activity endpoint called with id: {}", id);
        Activity activity = activityService.approveActivity(id, basePoints);
        return ResponseEntity.ok(ApiResponse.success("审核通过", mapToResponse(activity)));
    }

    /**
     * Publish an activity (owner or admin)
     */
    @PutMapping("/{id}/publish")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> publishActivity(@PathVariable Long id) {
        log.info("Publish activity endpoint called with id: {}", id);
        
        Activity activity = activityService.publishActivity(id);
        ActivityResponse response = mapToResponse(activity);
        
        return ResponseEntity.ok(ApiResponse.success("活动发布成功", response));
    }
    /**
     * Cancel an activity (owner or admin only)
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> cancelActivity(@PathVariable("id") Long id) {
        log.info("Cancel activity endpoint called with id: {}", id);
        activityService.cancelActivity(id);
        return ResponseEntity.ok(ApiResponse.success("活动下架成功"));
    }

    /**
     * Reject an activity application (admin only)
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> rejectActivity(@PathVariable Long id) {
        log.info("Reject activity endpoint called with id: {}", id);
        Activity activity = activityService.rejectActivity(id);
        return ResponseEntity.ok(ApiResponse.success("拒绝成功", mapToResponse(activity)));
    }
    
    /**
     * Update an activity (owner or admin)
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> updateActivity(
            @PathVariable Long id,
            @Valid @RequestBody ActivityUpdateRequest request) {
        log.info("Update activity endpoint called with id: {}", id);
        
        Activity activity = activityService.updateActivity(id, request);
        ActivityResponse response = mapToResponse(activity);
        
        return ResponseEntity.ok(ApiResponse.success("活动编辑成功", response));
    }
    @PutMapping("/updateById")
    public void updateActivityById(@RequestBody Activity activity) {
        activityMapper.updateById(activity);
    }
    /**
     * Delete an activity (owner or admin)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> deleteActivity(@PathVariable Long id) {
        log.info("Delete activity endpoint called with id: {}", id);
        
        activityService.deleteActivity(id);
        
        return ResponseEntity.ok(ApiResponse.success("活动删除成功"));
    }
    
    /**
     * Query activity list with pagination and filtering (public)
     * @param pageNum the page number (default 1)
     * @param pageSize the page size (default 10)
     * @param status the activity status (optional)
     * @param keyword the search keyword (optional)
     * @return the activity list response
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getActivityList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        log.info("Get activity list endpoint called - page: {}, size: {}, status: {}, keyword: {}", 
                pageNum, pageSize, status, keyword);
        
        Page<Activity> page = activityService.getActivityList(pageNum, pageSize, status, keyword);
        
        List<ActivityResponse> records = page.getRecords().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        ActivityListResponse response = ActivityListResponse.builder()
                .total(page.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .records(records)
                .build();
        
        return ResponseEntity.ok(ApiResponse.success("查询成功", response));
    }
    @GetMapping("/getList")
    public List<Activity> getActivitylist( Integer status,Integer delete){
        QueryWrapper<Activity> qw = new QueryWrapper<>();
        qw.eq("status", status)
                .eq("is_deleted", delete);
        List<Activity> activities = activityMapper.selectList(qw);
        return  activities;
    }
    @GetMapping("/getListByCreat")
    public List<Activity> getActivitylistByCreat( Long createdBy,Integer delete){
        System.out.println("活动获取到的userId为"+createdBy);
        QueryWrapper<Activity> activityQuery = new QueryWrapper<>();
        activityQuery.eq("created_by", createdBy).eq("is_deleted", delete);
        List<Activity> activities = activityMapper.selectList(activityQuery);
        System.out.println("活动获取到的list为"+activities);
        return  activities;
    }
    @GetMapping("/getMyList")
    public List<Activity> getMyActivitylist(){
        QueryWrapper<Activity> activityQuery = new QueryWrapper<>();
        activityQuery.eq("created_by", UserContext.getUser()).eq("is_deleted", 0);
        List<Activity> activities = activityMapper.selectList(activityQuery);

// 获取图片
        if (!activities.isEmpty()) {
            List<Long> activityIds = activities.stream().map(Activity::getId).toList();
            List<ActivityImg> imgs = activityMapper.getImg(activityIds);

            Map<Long, List<String>> imageMap = new HashMap<>();
            for (ActivityImg img : imgs) {
                if (img.getUrl() == null || img.getUrl().isEmpty()) continue;
                imageMap.computeIfAbsent(img.getActivityId(), k -> new ArrayList<>()).add(img.getUrl());
            }

            activities.forEach(activity ->
                    activity.setImageUrl(imageMap.getOrDefault(activity.getId(), Collections.emptyList()))
            );
        }
        return activities;
    }
    @PutMapping("/update")
    public void UpdateActivity(UpdateWrapper<Activity> qw){
        activityMapper.update(qw);
    }

    @GetMapping("/getDistance")
    public ApiResponse<?> getDistance(@RequestParam String start, @RequestParam String end){
    HashMap<String, String> result = new HashMap<>();
    result.put("origins", start);
    result.put("destination", end);
    result.put("type","0");
    result.put("key", "c060cf3dc2a148e9e8eba034a12efc57");
        String s = HttpClientUtil.doGet("https://restapi.amap.com/v3/distance", result);
        return ApiResponse.success("查询成功", s);
    }
    /**
     * Get activity detail (public)
     * @param id the activity ID
     * @return the activity response
     */
    @GetMapping("/{id}")
    public ApiResponse<?> getActivityDetail(@PathVariable Long id) {
        log.info("Get activity detail endpoint called with id: {}", id);
        
        Activity activity = activityService.getActivityDetail(id);
        
        return ApiResponse.success("查询成功", activity);
    }
    @PutMapping("/updateACById")
    public void updateAcById(@RequestParam("id")Long id,@RequestParam("registered_count")int registered_count) {
        UpdateWrapper<Activity> set = new UpdateWrapper<Activity>().eq("id", id).set("registered_count", registered_count);
        activityMapper.update(set);
    }
    /**
     * Map Activity entity to ActivityResponse DTO
     * @param activity the activity entity
     * @return the activity response
     */
    private ActivityResponse mapToResponse(Activity activity) {
        return ActivityResponse.builder()
                .id(activity.getId())
                .name(activity.getName())
                .description(activity.getDescription())
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .location(activity.getLocation())
                .requiredCount(activity.getRequiredCount())
                .registeredCount(activity.getRegisteredCount())
                .basePoints(activity.getBasePoints())
                .status(activity.getStatus())
                .createdBy(activity.getCreatedBy())
                .createTime(activity.getCreateTime())
                .updateTime(activity.getUpdateTime())
                .approved(activity.getApproved())
                .position(activity.getPosition())
                .imageUrl(activity.getImageUrl())
                .build();
    }
}
