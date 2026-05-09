package com.example.registrationservice.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyh.Client.ActivityClient;
import com.cyh.Client.UserClient;
import com.cyh.Res.ApiResponse;
import com.cyh.entity.Activity;
import com.cyh.entity.Registration;
import com.cyh.entity.RegistrationStatus;
import com.cyh.entity.User;
import com.example.registrationservice.Dto.RegistrationCreateRequest;
import com.example.registrationservice.Dto.RegistrationListResponse;
import com.example.registrationservice.Dto.RegistrationResponse;
import com.example.registrationservice.Service.RegistrationService;
import com.example.registrationservice.mapper.RegistrationMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Registration Controller
 *
 * Handles REST endpoints for registration management including submission,
 * cancellation, and querying.
 */
@Slf4j
@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    @Autowired
    private RegistrationService registrationService;
    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private ActivityClient activityClient;
    @Autowired
    private UserClient userClient;

    /**
     * Check if current user has registered for an activity
     *
     * @param activityId the activity ID
     * @return registration info or null
     */
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<?>> checkRegistration(@RequestParam Long activityId) {
        log.info("Check registration endpoint called for activity: {}", activityId);
        Registration registration = registrationService.checkRegistration(activityId);
        if (registration == null) {
            return ResponseEntity.ok(ApiResponse.success("未报名", null));
        }
        return ResponseEntity.ok(ApiResponse.success("已报名", mapToResponse(registration)));
    }

    /**
     * Submit a registration
     *
     * @param request the registration creation request
     * @return the created registration response
     */
    @PostMapping
    public ResponseEntity<ApiResponse<?>> submitRegistration(@Valid @RequestBody RegistrationCreateRequest request) {
        log.info("Submit registration endpoint called for activity: {}", request.getActivityId());

        Registration registration = registrationService.submitRegistration(request);
        RegistrationResponse response = mapToResponse(registration);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("报名成功", response));
    }

    /**
     * Cancel a registration
     *
     * @param id the registration ID
     * @return success response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> cancelRegistration(@PathVariable Long id) {
        log.info("Cancel registration endpoint called for registration: {}", id);

        registrationService.cancelRegistration(id);

        return ResponseEntity.ok(ApiResponse.success("取消报名成功"));
    }

    /**
     * Get personal registration list
     *
     * @param pageNum  the page number (default 1)
     * @param pageSize the page size (default 10)
     * @return the registration list response
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getMyRegistrations(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        log.info("Get my registrations endpoint called - page: {}, size: {}", pageNum, pageSize);

        Page<Registration> page = registrationService.getMyRegistrations(pageNum, pageSize);

        List<RegistrationResponse> records = page.getRecords().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        RegistrationListResponse response = RegistrationListResponse.builder()
                .total(page.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .records(records)
                .build();

        return ResponseEntity.ok(ApiResponse.success("查询成功", response));
    }
    @GetMapping("/getList")
    public List<Registration> getList(@RequestParam("activityId") Long activityId,@RequestParam("status") Integer status,@RequestParam("deleted") Integer deleted){
        QueryWrapper<Registration> regQw = new QueryWrapper<>();
        regQw.eq("activity_id", activityId)
                .eq("status", status)
                .eq("is_deleted", deleted);
        return registrationMapper.selectList(regQw  );
    }

    @GetMapping("/getCount")
    public Long getCount(@RequestParam("activityId") Long activityId,@RequestParam("status") Integer status,@RequestParam("deleted") Integer deleted){
        QueryWrapper<Registration> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("activity_id", activityId)
                .eq("status", status)
                .eq("is_deleted", deleted);
        return registrationMapper.selectCount(queryWrapper );
    }
    @GetMapping("getJoinUser")
    public List<User> getJoinUser(@RequestParam("activityId") Long activityId){
        QueryWrapper<Registration> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("activity_id", activityId)
                .eq("status", RegistrationStatus.APPROVED.getCode())
                .eq("is_deleted", 0)
                .orderByAsc("create_time");
        List<Registration> registrations = registrationMapper.selectList(queryWrapper);
        List<Long> users = registrations.stream().map(Registration::getUserId).toList();
        return users.stream().map(userId -> userClient.getUserName(userId)).toList();
    }
    @PutMapping("/update")
    public void updateRegistration(@RequestBody Registration registration){
         registrationMapper.updateById(registration);
    }
    @GetMapping("/getByQw")
    public List<Registration> getRegistration(@RequestParam("activityIds")List < Long> myActivityIds){
        QueryWrapper<Registration> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", RegistrationStatus.PENDING_REVIEW.getCode())
                .eq("is_deleted", 0)
                .orderByAsc("create_time");
          queryWrapper.in("activity_id", myActivityIds);
        return registrationMapper.selectList(queryWrapper);
    }

    /**
     * Get registration detail
     *
     * @param id the registration ID
     * @return the registration response
     */
    @GetMapping("/{id}")
    public ApiResponse<?> getRegistrationDetail(@PathVariable Long id) {
        log.info("Get registration detail endpoint called for registration: {}", id);

        Registration registration = registrationService.getRegistrationDetail(id);

        return ApiResponse.success("查询成功", registration);
    }
    @GetMapping("/getOne")
    public Registration getRegistrationOne(@RequestParam("activityId") Long activityId,@RequestParam("userId") Long userId,@RequestParam("status") Integer status) {
        QueryWrapper<Registration> wq = new QueryWrapper<Registration>().eq("activity_id", activityId).eq("user_id", userId).eq("status", status);
        return registrationMapper.selectOne(wq);
    }

    /**
     * Map Registration entity to RegistrationResponse DTO
     *
     * @param registration the registration entity
     * @return the registration response
     */
    private RegistrationResponse mapToResponse(Registration registration) {
        String activityName = null;
        Activity activity = activityClient.getActivityDetail(registration.getActivityId()).getData();
        if (activity != null) {
            activityName = activity.getName();
        }
        return RegistrationResponse.builder()
                .id(registration.getId())
                .userId(registration.getUserId())
                .activityId(registration.getActivityId())
                .activityName(activityName)
                .status(registration.getStatus())
                .rejectReason(registration.getRejectReason())
                .reviewedBy(registration.getReviewedBy())
                .reviewTime(registration.getReviewTime())
                .createTime(registration.getCreateTime())
                .build();
    }
}
