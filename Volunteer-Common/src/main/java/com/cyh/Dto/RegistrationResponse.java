package com.cyh.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Registration Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResponse {
    
    private Long id;
    private Long userId;
    private Long activityId;
    private String userName;
    private String activityName;
    private Integer status;
    private String rejectReason;
    private Long reviewedBy;
    private LocalDateTime reviewTime;
    private LocalDateTime createTime;
}
