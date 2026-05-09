package com.cyh.reviewservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Review Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private Long userId;
    private String userName;
    private Long activityId;
    private String activityName;
    private Integer status;
    private String rejectReason;
    private Long reviewedBy;
    private LocalDateTime reviewTime;
    private LocalDateTime createTime;
}
