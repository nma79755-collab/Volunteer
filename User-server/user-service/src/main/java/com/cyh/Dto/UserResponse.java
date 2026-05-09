package com.cyh.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    /**
     * User ID
     */
    private Long userId;
    
    /**
     * Username
     */
    private String username;
    
    /**
     * Real name
     */
    private String realName;
    private String sign;
    
    /**
     * Email
     */
    private String email;
    
    /**
     * Phone
     */
    private String phone;
    private Integer age;
    private Integer sex;
    private String city;
    private Integer checkcount;
    private Integer level;
    
    /**
     * User role
     */
    
    /**
     * Total points
     */
    private Long totalPoints;
    
    /**
     * Create time
     */
    private LocalDateTime createTime;
    private String avatar;
}
