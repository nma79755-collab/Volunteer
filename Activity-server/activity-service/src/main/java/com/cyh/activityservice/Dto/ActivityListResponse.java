package com.cyh.activityservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Activity List Response DTO
 * 
 * Used for returning paginated activity list to clients.
 * Wraps PageResponse with ActivityResponse records.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityListResponse {
    
    /**
     * Total number of activities
     */
    private long total;
    
    /**
     * Current page number
     */
    private int pageNum;
    
    /**
     * Page size
     */
    private int pageSize;
    
    /**
     * List of activity records
     */
    private List<ActivityResponse> records;
}
