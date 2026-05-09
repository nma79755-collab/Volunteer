package com.cyh.pointsservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Points Summary Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsSummaryResponse {

    private Long userId;
    private Long totalPoints;
    private LocalDateTime lastUpdateTime;
}
