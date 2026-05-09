package com.cyh.reviewservice.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyh.Client.ActivityClient;
import com.cyh.Client.UserClient;
import com.cyh.Dto.RegistrationListResponse;
import com.cyh.Dto.RegistrationResponse;
import com.cyh.Res.ApiResponse;
import com.cyh.entity.Activity;
import com.cyh.entity.Registration;
import com.cyh.entity.User;
import com.cyh.reviewservice.Dto.ReviewBatchRequest;
import com.cyh.reviewservice.Dto.ReviewBatchResponse;
import com.cyh.reviewservice.Dto.ReviewRejectRequest;
import com.cyh.reviewservice.Dto.ReviewResponse;
import com.cyh.reviewservice.Service.ReviewService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Review Controller
 *
 * Handles REST endpoints for admin review of volunteer registrations,
 * including approve, reject, pending list, and batch operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ActivityClient activityClient;

    @Autowired
    private UserClient userClient;

    /**
     * Approve a registration
     *
     * @param registrationId the registration ID
     * @return success response
     */
    @PutMapping("/{registrationId}/approve")
    public ResponseEntity<ApiResponse<?>> approveRegistration(@PathVariable Long registrationId) {
        log.info("Approve registration endpoint called for registration: {}", registrationId);

        Registration registration = reviewService.approveRegistration(registrationId);
        ReviewResponse response = mapToResponse(registration);

        return ResponseEntity.ok(ApiResponse.success("批准成功", response));
    }

    /**
     * Reject a registration
     *
     * @param registrationId the registration ID
     * @param request        the reject request containing the reason
     * @return success response
     */
    @PutMapping("/{registrationId}/reject")
    public ResponseEntity<ApiResponse<?>> rejectRegistration(
            @PathVariable Long registrationId,
            @Valid @RequestBody ReviewRejectRequest request) {
        log.info("Reject registration endpoint called for registration: {}", registrationId);

        Registration registration = reviewService.rejectRegistration(registrationId, request.getRejectReason());
        ReviewResponse response = mapToResponse(registration);

        return ResponseEntity.ok(ApiResponse.success("拒绝成功", response));
    }

    /**
     * Get pending registrations list
     *
     * @param pageNum  the page number (default 1)
     * @param pageSize the page size (default 10)
     * @return paginated pending registrations
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<?>> getPendingRegistrations(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        log.info("Get pending registrations endpoint called - page: {}, size: {}", pageNum, pageSize);

        Page<Registration> page = reviewService.getPendingRegistrations(pageNum, pageSize);

        List<ReviewResponse> records = page.getRecords().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        RegistrationListResponse response = RegistrationListResponse.builder()
                .total(page.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .records(records.stream()
                        .map(r -> RegistrationResponse.builder()
                                .id(r.getId())
                                .userId(r.getUserId())
                                .userName(userClient.getUserById(r.getUserId()).getRealName())
                                .activityId(r.getActivityId())
                                .activityName(r.getActivityName())
                                .status(r.getStatus())
                                .rejectReason(r.getRejectReason())
                                .reviewedBy(r.getReviewedBy())
                                .reviewTime(r.getReviewTime())
                                .createTime(r.getCreateTime())
                                .build())
                        .collect(Collectors.toList()))
                .build();

        return ResponseEntity.ok(ApiResponse.success("查询成功", response));
    }

    /**
     * Batch approve or reject registrations
     *
     * @param request the batch review request
     * @return batch result
     */
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<?>> batchReview(@Valid @RequestBody ReviewBatchRequest request) {
        log.info("Batch review endpoint called: action={}, count={}", request.getAction(), request.getRegistrationIds().size());

        ReviewBatchResponse response = reviewService.batchReview(request);

        return ResponseEntity.ok(ApiResponse.success("批量审核成功", response));
    }

    /**
     * Map Registration entity to ReviewResponse DTO
     *
     * @param registration the registration entity
     * @return the review response
     */
    private ReviewResponse mapToResponse(Registration registration) {
        String userName = null;
        User user = userClient.getUserById(registration.getUserId());
        if (user != null) {
            userName = user.getRealName() != null ? user.getRealName() : user.getUsername();
        }

        String activityName = null;
        Activity activity = activityClient.getActivityDetail(registration.getActivityId()).getData();
        if (activity != null) {
            activityName = activity.getName();
        }

        return ReviewResponse.builder()
                .id(registration.getId())
                .userId(registration.getUserId())
                .userName(userName)
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
