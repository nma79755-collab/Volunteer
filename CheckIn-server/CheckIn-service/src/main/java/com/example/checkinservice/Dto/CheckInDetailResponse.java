package com.example.checkinservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Check-in Detail Response DTO (used in paginated list)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInDetailResponse {

    private Long id;
    private Long registrationId;
    private Long userId;
    private String userName;
    private Long activityId;
    private String activityName;
    private Integer status;
    private String statusDesc;
    private LocalDateTime checkInTime;
    private LocalDateTime createTime;
}
