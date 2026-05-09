package com.cyh.activityservice.Dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Activity Response DTO
 * 
 * Used for returning activity information to clients.
 * Contains all activity details including status and registration info.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityResponse {
    
    /**
     * Activity ID
     */
    private Long id;
    
    /**
     * Activity name
     */
    private String name;
    
    /**
     * Activity description
     */
    private String description;
    
    /**
     * Activity start time
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    
    /**
     * Activity end time
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    
    /**
     * Activity location
     */
    private String location;
    private List<String> imageUrl;

    /**
     * Required number of volunteers
     */
    private Integer requiredCount;
    private String position;
    
    /**
     * Number of registered volunteers
     */
    private Integer registeredCount;
    
    /**
     * Base points for completing the activity
     */
    private Integer basePoints;
    
    /**
     * Activity status (0-draft, 1-published, 2-in_progress, 3-completed, 4-cancelled)
     */
    private Integer status;
    
    /**
     * ID of the user who created the activity
     */
    private Long createdBy;
    
    /**
     * Activity creation time
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    /**
     * Approval status: 0=pending, 1=approved
     */
    private Integer approved;
}
