package com.example.checkinservice.Controller;

import com.cyh.Dto.PageResponse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyh.Client.ActivityClient;
import com.cyh.Res.ApiResponse;
import com.cyh.Utils.QrCodeUtil;
import com.cyh.entity.Activity;
import com.cyh.entity.CheckIn;
import com.example.checkinservice.Dto.CheckInDetailResponse;
import com.example.checkinservice.Dto.CheckInStatisticsResponse;
import com.example.checkinservice.Service.CheckInService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Check-in Controller
 *
 * Handles REST endpoints for volunteer check-in management.
 * Requirement: 4
 */
@Slf4j
@RestController
@RequestMapping("/api/check-ins")
public class CheckInController {

    @Autowired
    private CheckInService checkInService;
    @Autowired
    private ActivityClient activityClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    /**
     * Perform check-in for a registration.
     *
     * POST /api/check-ins
     *
     * @return the check-in response
     *
     */
    @PostMapping
    public ResponseEntity<ApiResponse<?>> checkIn(@RequestParam("id") Long id) {
        log.info("解析出的id为"+id);
        Integer checkIn = checkInService.addCheckIn(id);
        if(checkIn==1){
            return ResponseEntity.ok(ApiResponse.success("签到成功"));
        }
        else  if(checkIn==2){
            return ResponseEntity.ok(ApiResponse.error("您已经签到过了"));
        }
        return ResponseEntity.ok(ApiResponse.error("您未报名该活动"));
    }
    @GetMapping("/getCode/{activityId}")
    @Operation(summary = "生成签到二维码")
    public void getCode(@PathVariable("activityId")String id, HttpServletResponse response) throws Exception {
        Activity activityDetail = activityClient.getActivityDetail(Long.valueOf(id)).getData();
        if(activityDetail==null){
            return;
        }
        BufferedImage img = QrCodeUtil.generate(id, 300, 300);
        response.setContentType("image/png");
        ServletOutputStream outputStream = response.getOutputStream();
        log.info("id为"+id+"的二维码生成完成");
        ImageIO.write(img, "png", outputStream);
        outputStream.flush();
        outputStream.close();
    }
    @GetMapping("/getList")
    public List<CheckIn> getList(Long activityId){
        return  checkInService.getCheckInList(activityId);
    }
    @GetMapping("/getNum/{activityId}")
    public ResponseEntity<ApiResponse<?>> getNum(@PathVariable("activityId")Long activityId){
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 10)+activityId;
        log.info("活动id为"+activityId+"的代码生成完毕,为"+code);
        String key="activityId"+activityId;
        Activity activityDetail = activityClient.getActivityDetail(activityId).getData();
        LocalDateTime endTime = activityDetail.getEndTime();
        if (LocalDateTime.now().isAfter(endTime)) {
            return ResponseEntity.ok(ApiResponse.error("活动已结束"));
        }
        // 5. 计算剩余有效秒数（保证是正数）
        long expireSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), endTime);

        // 6. 存入Redis（如果已存在则不覆盖，保证幂等）
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.opsForValue().set(key, code, expireSeconds, TimeUnit.SECONDS);
        }
        else if(Boolean.TRUE.equals(redisTemplate.hasKey(key)) ){
             code =redisTemplate.opsForValue().get(key);
        }
        return ResponseEntity.ok(ApiResponse.success(code));
    }
    @GetMapping("/checkByNum/{Num}")
    public ResponseEntity<ApiResponse<?>> checkByNum(@PathVariable("Num")String num){
        // 从第 11 位开始取到末尾
        String result = num.substring(10);
        String key="activityId"+result;
        String o = redisTemplate.opsForValue().get(key);
        if (o != null && o.equals(num)) {
            Integer checkIn = checkInService.addCheckIn(Long.valueOf(result));
            if(checkIn==1){
                return ResponseEntity.ok(ApiResponse.success("签到成功"));
            }
            else  if(checkIn==2){
                return ResponseEntity.ok(ApiResponse.error("您已经签到过了"));
            }
            return ResponseEntity.ok(ApiResponse.error("您未报名该活动"));
        }
        return ResponseEntity.ok(ApiResponse.error("签到失败,请检查代码是否正确"));
    }
    @GetMapping("/checked/{activityId}")
    @Operation(summary = "检查用户是否已签到")
    public ResponseEntity<ApiResponse<?>> checked(@PathVariable("activityId")Long activityId){
        Integer checked = checkInService.Checked(activityId);
        if(checked==1){
            return ResponseEntity.ok(ApiResponse.success("用户已签到",1));
        }
        return ResponseEntity.ok(ApiResponse.error("用户未签到"));
    }
    /**
     * Get check-in statistics for an activity.
     *
     * GET /api/check-ins/statistics/{activityId}
     *
     * @param activityId the activity ID
     * @return statistics response
     */
    @GetMapping("/statistics/{activityId}")
    public ResponseEntity<ApiResponse<?>> getStatistics(@PathVariable Long activityId) {
        log.info("Get check-in statistics endpoint called for activity: {}", activityId);

        CheckInStatisticsResponse statistics = checkInService.getStatistics(activityId);

        return ResponseEntity.ok(ApiResponse.success("查询成功", statistics));
    }

    /**
     * Get paginated check-in details for an activity.
     *
     * GET /api/check-ins/{activityId}/details
     *
     * @param activityId the activity ID
     * @param pageNum    page number (default 1)
     * @param pageSize   page size (default 10)
     * @return paginated check-in details
     */
    @GetMapping("/{activityId}/details")
    public ResponseEntity<ApiResponse<?>> getDetails(
            @PathVariable Long activityId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        log.info("Get check-in details endpoint called for activity: {} - page: {}, size: {}", activityId, pageNum, pageSize);

        Page<CheckInDetailResponse> page = checkInService.getDetails(activityId, pageNum, pageSize);

        PageResponse<CheckInDetailResponse> response = PageResponse.<CheckInDetailResponse>builder()
                .total(page.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .records(page.getRecords())
                .build();

        return ResponseEntity.ok(ApiResponse.success("查询成功", response));
    }

    /**
     * Map CheckIn entity to CheckInResponse DTO.
     */
}
