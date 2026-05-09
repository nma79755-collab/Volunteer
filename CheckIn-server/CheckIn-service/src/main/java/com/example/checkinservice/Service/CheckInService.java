package com.example.checkinservice.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.cyh.Client.ActivityClient;
import com.cyh.Client.PointsClient;
import com.cyh.Client.RegistrationClient;
import com.cyh.Client.UserClient;
import com.cyh.Utils.JwtUserDetails;
import com.cyh.entity.*;
import com.cyh.enums.ActivityStatus;
import com.cyh.exception.BusinessException;
import com.example.checkinservice.Dto.CheckInDetailResponse;
import com.example.checkinservice.Dto.CheckInStatisticsResponse;
import com.example.checkinservice.enums.CheckInStatus;
import com.example.checkinservice.mapper.CheckInMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Check-in Service
 *
 * Handles business logic for volunteer check-in management.
 * Requirement: 4
 */
@Slf4j
@Service
public class CheckInService {

    @Autowired
    private CheckInMapper checkInMapper;

    @Autowired
    private RegistrationClient registrationClient;

    @Autowired
    private ActivityClient activityClient;

    @Autowired
    private UserClient userClient;

    @Autowired
    private PointsClient pointsClient;

    /**
     * Perform check-in for a registration.
     * @return the created/updated CheckIn record
     * @throws BusinessException if validation fails
     */
    public Integer addCheckIn(Long activityId){
        Long userId = getCurrentUserId();
        Registration registration = registrationClient.getRegistrationOne(activityId,userId,1);
        if(registration==null){
            return 0;
        }
        CheckIn checkIn1 = checkInMapper.selectOne(new QueryWrapper<CheckIn>().eq("activity_id", activityId).eq("user_id", userId));
        if(checkIn1!=null){
            return 2;
        }
        CheckIn checkIn = new CheckIn();
        Activity activity = activityClient.getActivityDetail(activityId).getData();
        if(activity.getStartTime().plusMinutes(10).isBefore(LocalDateTime.now())){
            checkIn.setStatus(2);
        }
        Long id = registration.getId();
        checkIn.setCreateTime(activity.getStartTime());
        if(activity.getStartTime().plusMinutes(10).isAfter(LocalDateTime.now())&&LocalDateTime.now().isBefore(activity.getStartTime().plusMinutes(30))){
            checkIn.setStatus(1);
        }
        if(activity.getStartTime().plusMinutes(30).isBefore(LocalDateTime.now())){
            checkIn.setStatus(0);
        }
        checkIn.setUpdateTime(LocalDateTime.now());
        checkIn.setActivityId(activityId);
        checkIn.setRegistrationId(id);
        checkIn.setUserId(userId);
        checkIn.setIsDeleted(0);
        checkIn.setCheckInTime(LocalDateTime.now());
        checkInMapper.insert(checkIn);
        pointsClient.allocatePoints(checkIn);
        return 1;
    }
    /**
     * Get check-in statistics for an activity.
     *
     * @param activityId the activity ID
     * @return statistics with checked-in, late, and absent counts
     */
    public List<CheckIn> getCheckInList(Long activityId) {
        return checkInMapper.findByActivityId(activityId);
    }
    public CheckInStatisticsResponse getStatistics(Long activityId) {
        log.info("Getting check-in statistics for activity {}", activityId);

        Activity activity = getActivityOrThrow(activityId);

        List<CheckIn> checkIns = checkInMapper.findByActivityId(activityId);

        long checkedInCount = checkIns.stream()
                .filter(c -> c.getStatus() == CheckInStatus.CHECKED_IN.getCode())
                .count();
        long lateCount = checkIns.stream()
                .filter(c -> c.getStatus() == CheckInStatus.LATE.getCode())
                .count();
        long absentCount = checkIns.stream()
                .filter(c -> c.getStatus() == CheckInStatus.ABSENT.getCode())
                .count();

        // Total approved registrations
        long totalApproved = registrationClient.getCount(activityId,RegistrationStatus.APPROVED.getCode(),0);

        return CheckInStatisticsResponse.builder()
                .activityId(activityId)
                .activityName(activity.getName())
                .checkedInCount(checkedInCount)
                .lateCount(lateCount)
                .absentCount(absentCount)
                .totalApproved(totalApproved)
                .build();
    }
    public Integer Checked(Long activityId){
        Long currentUserId = getCurrentUserId();
        CheckIn checkIn = checkInMapper.selectOne(new QueryWrapper<CheckIn>().eq("activity_id", activityId).eq("user_id", currentUserId));
        if (checkIn!=null){
            return 1;
        }
        return 0;
    }
    /**
     * Get paginated check-in details for an activity.
     *
     * @param activityId the activity ID
     * @param pageNum    page number
     * @param pageSize   page size
     * @return paginated list of check-in details
     */
    public Page<CheckInDetailResponse> getDetails(Long activityId, int pageNum, int pageSize) {
        log.info("Getting check-in details for activity {} - page: {}, size: {}", activityId, pageNum, pageSize);

        Activity activity = getActivityOrThrow(activityId);

        QueryWrapper<CheckIn> qw = new QueryWrapper<>();
        qw.eq("activity_id", activityId)
                .eq("is_deleted", 0)
                .orderByDesc("create_time");

        Page<CheckIn> checkInPage = new Page<>(pageNum, pageSize);
        checkInMapper.selectPage(checkInPage, qw);

        List<CheckInDetailResponse> records = checkInPage.getRecords().stream()
                .map(c -> mapToDetailResponse(c, activity))
                .collect(Collectors.toList());

        Page<CheckInDetailResponse> resultPage = new Page<>(pageNum, pageSize);
        resultPage.setTotal(checkInPage.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    /**
     * Mark absent: set all NOT_CHECKED_IN registrations for a completed activity as ABSENT.
     *
     * @param activityId the activity ID
     * @return number of registrations marked absent
     */
    @Transactional
    public int markAbsent(Long activityId) {
        log.info("Marking absent for activity {}", activityId);

        Activity activity = getActivityOrThrow(activityId);

        if (activity.getStatus() != ActivityStatus.COMPLETED.getCode()) {
            throw new BusinessException("只有已结束的活动才能标记缺席");
        }

        // Find all approved registrations for this activity

        List<Registration> approvedRegistrations = registrationClient.getList(activityId,RegistrationStatus.APPROVED.getCode(),0);

        int markedCount = 0;
        LocalDateTime now = LocalDateTime.now();

        for (Registration reg : approvedRegistrations) {
            CheckIn existing = checkInMapper.findByRegistrationId(reg.getId());

            if (existing == null) {
                // No check-in record at all — create one as ABSENT
                CheckIn absent = CheckIn.builder()
                        .registrationId(reg.getId())
                        .userId(reg.getUserId())
                        .activityId(activityId)
                        .status(CheckInStatus.ABSENT.getCode())
                        .createTime(now)
                        .updateTime(now)
                        .isDeleted(0)
                        .build();
                checkInMapper.insert(absent);
                markedCount++;
            } else if (existing.getStatus() == CheckInStatus.NOT_CHECKED_IN.getCode()) {
                // Existing NOT_CHECKED_IN record — update to ABSENT
                existing.setStatus(CheckInStatus.ABSENT.getCode());
                existing.setUpdateTime(now);
                checkInMapper.updateById(existing);
                markedCount++;
            }
        }

        log.info("Marked {} registrations as absent for activity {}", markedCount, activityId);
        return markedCount;
    }

    /**
     * Determine if a check-in time is late relative to the activity start time.
     *
     * @param checkInTime     the time of check-in
     * @param activityStartTime the activity start time
     * @return true if the check-in is after the activity start time
     */
    public boolean isLate(LocalDateTime checkInTime, LocalDateTime activityStartTime) {
        return checkInTime.isAfter(activityStartTime);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Registration getRegistrationOrThrow(Long registrationId) {
        Registration registration = registrationClient.getRegistrationDetail(registrationId).getBody().getData();
        if (registration == null || registration.getIsDeleted() == 1) {
            throw new BusinessException("报名记录不存在");
        }
        return registration;
    }

    private Activity getActivityOrThrow(Long activityId) {
        Activity activity = activityClient.getActivityDetail(activityId).getData();
        if (activity == null || activity.getIsDeleted() == 1) {
            throw new BusinessException("活动不存在");
        }
        return activity;
    }

    private CheckInDetailResponse mapToDetailResponse(CheckIn checkIn, Activity activity) {
        String userName = null;
        User user = userClient.getUserById(checkIn.getUserId());
        if (user != null) {
            userName = user.getRealName() != null ? user.getRealName() : user.getUsername();
        }

        CheckInStatus statusEnum = CheckInStatus.getByCode(checkIn.getStatus());
        String statusDesc = statusEnum != null ? statusEnum.getDesc() : String.valueOf(checkIn.getStatus());

        return CheckInDetailResponse.builder()
                .id(checkIn.getId())
                .registrationId(checkIn.getRegistrationId())
                .userId(checkIn.getUserId())
                .userName(userName)
                .activityId(checkIn.getActivityId())
                .activityName(activity.getName())
                .status(checkIn.getStatus())
                .statusDesc(statusDesc)
                .checkInTime(checkIn.getCheckInTime())
                .createTime(checkIn.getCreateTime())
                .build();
    }

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
}
