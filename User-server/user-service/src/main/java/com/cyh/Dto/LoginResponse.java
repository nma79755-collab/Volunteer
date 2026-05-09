package com.cyh.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    
    /**
     * JWT token
     */
    private String token;
    
    /**
     * User ID
     */
    private Long userId;
    
    /**
     * Username
     */
    private String username;
    
    /**
     * User role
     */
    private Integer role;
    
    /**
     * Total points
     */
    private Long totalPoints;
    
    /**
     * Token expiration time in seconds
     */
    private Long expiresIn;
}
