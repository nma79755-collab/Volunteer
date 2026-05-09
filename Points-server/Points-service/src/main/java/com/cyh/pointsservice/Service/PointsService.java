package com.cyh.pointsservice.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyh.Client.ActivityClient;
import com.cyh.Client.CheckInClient;
import com.cyh.Client.RegistrationClient;
import com.cyh.Client.UserClient;
import com.cyh.Dto.PageResponse;
import com.cyh.entity.Activity;
import com.cyh.entity.CheckIn;
import com.cyh.entity.User;
import com.cyh.enums.CheckInStatus;
import com.cyh.exception.BusinessException;
import com.cyh.pointsservice.Dto.PointsDetailResponse;
import com.cyh.pointsservice.Dto.PointsRankingResponse;
import com.cyh.pointsservice.Dto.PointsSummaryResponse;
import com.cyh.pointsservice.entity.Points;
import com.cyh.pointsservice.mapper.PointsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Points Service
 *
 * Handles business logic for points calculation and distribution.
 * Requirement: 5
 */
@Slf4j
@Service
public class PointsService {

    @Autowired
    private PointsMapper pointsMapper;

    @Autowired
    private UserClient userClient;

    @Autowired
    private CheckInClient checkInClient;

    @Autowired
    private RegistrationClient registrationClient;

    @Autowired
    private ActivityClient activityClient;

    @Autowired
    private PointsCalculator pointsCalculator;

    /**
     * Calculate points based on check-in status.
     *
     * @param checkIn the check-in record
     * @return calculated points
     */
    public int calculatePoints(CheckIn checkIn) {
        Activity activity = activityClient.getActivityDetail(checkIn.getActivityId()).getData();
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        return pointsCalculator.calculate(activity.getBasePoints(), checkIn.getStatus());
    }

    /**
     * Allocate points to a user for a specific check-in.
     *
     * @param checkIn the check-in record
     * @return the created Points record
     */
    @Transactional
    public Points allocatePoints(CheckIn checkIn) {
        log.info("Allocating points for check-in {}", checkIn.getId());

        int pointsEarned = calculatePoints(checkIn);
        
        if (pointsEarned == 0) {
            log.info("No points to allocate for check-in {} (status: {})", 
                    checkIn.getId(), checkIn.getStatus());
            return null;
        }

        // Create points record
        String reason = getPointsReason(checkIn.getStatus());
        Points points = Points.builder()
                .userId(checkIn.getUserId())
                .activityId(checkIn.getActivityId())
                .registrationId(checkIn.getRegistrationId())
                .pointsEarned(pointsEarned)
                .reason(reason)
                .createTime(LocalDateTime.now())
                .isDeleted(0)
                .build();

        pointsMapper.insert(points);

        // Update user total points
        User user = userClient.getUserById(checkIn.getUserId());
        if (user != null) {
            Long currentPoints = user.getTotalPoints() != null ? user.getTotalPoints() : 0L;
            user.setTotalPoints(currentPoints + pointsEarned);
            user.setUpdateTime(LocalDateTime.now());
            userClient.UpdateUserById(user);
        }

        log.info("Allocated {} points to user {} for activity {}", 
                pointsEarned, checkIn.getUserId(), checkIn.getActivityId());

        return points;
    }

    /**
     * Get user's total points summary.
     *
     * @param userId the user ID
     * @return PointsSummaryResponse with userId, totalPoints, and lastUpdateTime
     */
    public PointsSummaryResponse getUserTotalPoints(Long userId) {
        User user = userClient.getUserById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return PointsSummaryResponse.builder()
                .userId(userId)
                .totalPoints(user.getTotalPoints() != null ? user.getTotalPoints() : 0L)
                .lastUpdateTime(user.getUpdateTime())
                .build();
    }

    /**
     * Get user's points details with pagination.
     *
     * @param userId   the user ID
     * @param pageNum  page number
     * @param pageSize page size
     * @return paginated points details
     */
    public PageResponse<PointsDetailResponse> getUserPointsDetails(Long userId, int pageNum, int pageSize) {
        log.info("Getting points details for user {} - page: {}, size: {}", userId, pageNum, pageSize);

        QueryWrapper<Points> qw = new QueryWrapper<>();
        qw.eq("user_id", userId)
                .eq("is_deleted", 0)
                .orderByDesc("create_time");

        Page<Points> pointsPage = new Page<>(pageNum, pageSize);
        pointsMapper.selectPage(pointsPage, qw);

        List<PointsDetailResponse> records = pointsPage.getRecords().stream()
                .map(this::mapToPointsDetail)
                .collect(Collectors.toList());

        return PageResponse.<PointsDetailResponse>builder()
                .total(pointsPage.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .records(records)
                .build();
    }

    /**
     * Get points ranking with pagination.
     *
     * @param pageNum  page number
     * @param pageSize page size
     * @return paginated points ranking
     */
    public PageResponse<PointsRankingResponse> getPointsRanking(int pageNum, int pageSize) {
        log.info("Getting points ranking - page: {}, size: {}", pageNum, pageSize);



        Page<User> userPage = new Page<>(pageNum, pageSize);
        List<User> userlist = userClient.getUserByIdPage();
        userPage.setRecords(userlist);
        userPage.setTotal(userlist.size());
        int startRank = (pageNum - 1) * pageSize + 1;
        List<User> users = userPage.getRecords();
        List<PointsRankingResponse> records = new java.util.ArrayList<>();
        for (int i = 0; i < users.size(); i++) {
            records.add(mapToRankingDetail(users.get(i), startRank + i));
        }

        return PageResponse.<PointsRankingResponse>builder()
                .total(userPage.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .records(records)
                .build();
    }

    /**
     * Asynchronously distribute points for all check-ins of a completed activity.
     *
     * @param activityId the activity ID
     */
    @Async
    @Transactional
    public void distributePointsAsync(Long activityId) {
        log.info("Starting async points distribution for activity {}", activityId);

        Activity activity = activityClient.getActivityDetail(activityId).getData();
        if (activity == null) {
            log.warn("Activity not found: {}", activityId);
            return;
        }

        // Get all check-ins for this activity
        List<CheckIn> checkIns = checkInClient.getList(activityId);
        
        int successCount = 0;
        int skipCount = 0;

        for (CheckIn checkIn : checkIns) {
            try {
                // Check if points already allocated
                QueryWrapper<Points> qw = new QueryWrapper<>();
                qw.eq("registration_id", checkIn.getRegistrationId())
                        .eq("is_deleted", 0);
                Long existingCount = pointsMapper.selectCount(qw);

                if (existingCount > 0) {
                    log.debug("Points already allocated for registration {}", checkIn.getRegistrationId());
                    skipCount++;
                    continue;
                }

                // Allocate points
                Points points = allocatePoints(checkIn);
                if (points != null) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("Failed to allocate points for check-in {}", checkIn.getId(), e);
            }
        }

        log.info("Completed async points distribution for activity {}: {} allocated, {} skipped", 
                activityId, successCount, skipCount);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String getPointsReason(int checkInStatus) {
        if (checkInStatus == CheckInStatus.CHECKED_IN.getCode()) {
            return "正常完成";
        } else if (checkInStatus == CheckInStatus.LATE.getCode()) {
            return "迟到完成";
        } else {
            return "缺席";
        }
    }

    private PointsDetailResponse mapToPointsDetail(Points points) {
        String activityName = null;
        Activity activity = activityClient.getActivityDetail(points.getActivityId()).getData();
        if (activity != null) {
            activityName = activity.getName();
        }
        return PointsDetailResponse.builder()
                .id(points.getId())
                .activityId(points.getActivityId())
                .activityName(activityName)
                .pointsEarned(points.getPointsEarned())
                .reason(points.getReason())
                .createTime(points.getCreateTime())
                .build();
    }

    private PointsRankingResponse mapToRankingDetail(User user, int rank) {
        return PointsRankingResponse.builder()
                .rank(rank)
                .userId(user.getId())
                .userName(user.getRealName() != null ? user.getRealName() : user.getUsername())
                .avatar(user.getAvatar())
                .totalPoints(user.getTotalPoints() != null ? user.getTotalPoints() : 0L)
                .build();
    }
}
