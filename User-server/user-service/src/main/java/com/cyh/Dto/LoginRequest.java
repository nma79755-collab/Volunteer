package com.cyh.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * Login Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    
    /**
     * Username
     */
    @NotBlank(message = "Username cannot be blank")
    private String username;
    
    /**
     * Password
     */
    @NotBlank(message = "Password cannot be blank")
    private String password;
}
