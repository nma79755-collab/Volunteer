package com.cyh.activityservice.Dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Activity Update Request DTO
 * 
 * Used for updating existing volunteer activities.
 * Only draft status activities can be updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityUpdateRequest {
    
    /**
     * Activity name
     */
    @NotBlank(message = "活动名称不能为空")
    @Size(min = 2, max = 100, message = "活动名称长度应在2-100之间")
    private String name;
    
    /**
     * Activity description
     */
    @NotBlank(message = "活动描述不能为空")
    @Size(min = 10, max = 1000, message = "活动描述长度应在10-1000之间")
    private String description;
    
    /**
     * Activity start time
     */
    @NotNull(message = "开始时间不能为空")
    @Future(message = "开始时间必须是未来时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    private List<String> imageUrls;
    
    /**
     * Activity end time
     */
    @NotNull(message = "结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    
    /**
     * Activity location
     */
    @NotBlank(message = "活动地点不能为空")
    @Size(min = 2, max = 200, message = "活动地点长度应在2-200之间")
    private String location;
    
    /**
     * Required number of volunteers
     */
    @NotNull(message = "需求人数不能为空")
    @Min(value = 1, message = "需求人数至少为1")
    @Max(value = 10000, message = "需求人数不能超过10000")
    private Integer requiredCount;
    
    /**
     * Base points for completing the activity (admin only, ignored for non-admin)
     */
    @Min(value = 0, message = "基础积分不能为负数")
    @Max(value = 1000, message = "基础积分不能超过1000")
    private Integer basePoints;
}
