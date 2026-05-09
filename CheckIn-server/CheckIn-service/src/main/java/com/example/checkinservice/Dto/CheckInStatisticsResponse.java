package com.example.checkinservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Check-in Statistics Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInStatisticsResponse {

    private Long activityId;
    private String activityName;

    /** Number of volunteers who checked in on time */
    private long checkedInCount;

    /** Number of volunteers who checked in late */
    private long lateCount;

    /** Number of volunteers who were absent */
    private long absentCount;

    /** Total approved registrations */
    private long totalApproved;
}
