package com.cyh.pointsservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Points Ranking Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsRankingResponse {

    private Integer rank;
    private Long userId;
    private String userName;
    private String avatar;
    private Long totalPoints;
}
