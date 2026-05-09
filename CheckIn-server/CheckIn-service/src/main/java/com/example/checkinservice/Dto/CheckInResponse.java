package com.example.checkinservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Check-in Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInResponse {

    private Long id;
    private Long registrationId;
    private Long userId;
    private Long activityId;
    private Integer status;
    private String statusDesc;
    private LocalDateTime checkInTime;
    private LocalDateTime createTime;
}
