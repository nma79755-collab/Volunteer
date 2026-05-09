package com.cyh.pointsservice.Controller;


import com.cyh.Client.ActivityClient;
import com.cyh.Dto.PageResponse;
import com.cyh.Res.ApiResponse;
import com.cyh.Utils.JwtUserDetails;
import com.cyh.entity.CheckIn;
import com.cyh.exception.BusinessException;
import com.cyh.pointsservice.Dto.PointsDetailResponse;
import com.cyh.pointsservice.Dto.PointsRankingResponse;
import com.cyh.pointsservice.Dto.PointsSummaryResponse;
import com.cyh.pointsservice.Service.PointsService;
import com.cyh.pointsservice.entity.Points;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Points Controller
 *
 * Handles REST endpoints for volunteer points queries.
 * Requirement: 5
 */
@Slf4j
@RestController
@RequestMapping("/api/points")
public class PointsController {

    @Autowired
    private PointsService pointsService;

    /**
     * Get current user's points summary.
     *
     * GET /api/points/summary
     *
     * @return points summary response
     */

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<PointsSummaryResponse>> getSummary() {
        Long userId = getCurrentUserId();
        log.info("Get points summary for user {}", userId);

        PointsSummaryResponse summary = pointsService.getUserTotalPoints(userId);
        return ResponseEntity.ok(ApiResponse.success("查询成功", summary));
    }

    /**
     * Get current user's points details with pagination.
     *
     * GET /api/points/details
     *
     * @param page     page number (default 1)
     * @param pageSize page size (default 10)
     * @return paginated points details
     */
    @GetMapping("/details")
    public ResponseEntity<ApiResponse<PageResponse<PointsDetailResponse>>> getDetails(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = getCurrentUserId();
        log.info("Get points details for user {} - page: {}, size: {}", userId, page, pageSize);

        PageResponse<PointsDetailResponse> details = pointsService.getUserPointsDetails(userId, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success("查询成功", details));
    }

    /**
     * Get points ranking with pagination.
     *
     * GET /api/points/ranking
     *
     * @param page     page number (default 1)
     * @param pageSize page size (default 10)
     * @return paginated points ranking
     */
    @GetMapping("/ranking")
    public ResponseEntity<ApiResponse<PageResponse<PointsRankingResponse>>> getRanking(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        log.info("Get points ranking - page: {}, size: {}", page, pageSize);

        PageResponse<PointsRankingResponse> ranking = pointsService.getPointsRanking(page, pageSize);
        return ResponseEntity.ok(ApiResponse.success("查询成功", ranking));
    }
    @GetMapping("/allocatePoints")
    public Points allocatePoints(@RequestBody CheckIn checkIn){
        return pointsService.allocatePoints(checkIn);
    }


    /**
     * Extract current authenticated user's ID from Spring Security context.
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof JwtUserDetails) {
            return ((JwtUserDetails) authentication.getDetails()).getUserId();
        }
        throw new BusinessException("用户未认证");
    }
}
