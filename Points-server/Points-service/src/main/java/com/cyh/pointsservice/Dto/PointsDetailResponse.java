package com.cyh.pointsservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Points Detail Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsDetailResponse {

    private Long id;
    private Long activityId;
    private String activityName;
    private Integer pointsEarned;
    private String reason;
    private LocalDateTime createTime;
}
