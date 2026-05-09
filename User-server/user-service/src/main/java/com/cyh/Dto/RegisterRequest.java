package com.cyh.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Register Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    
    /**
     * Username
     */
    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username length should be between 3 and 50")
    private String username;
    
    /**
     * Password
     */
    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, max = 100, message = "Password length should be between 6 and 100")
    private String password;
    
    /**
     * Real name
     */
    @NotBlank(message = "Real name cannot be blank")
    @Size(min = 2, max = 50, message = "Real name length should be between 2 and 50")
    private String realName;
    
    /**
     * Email
     */
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email format is invalid")
    private String email;
    
    /**
     * Phone
     */
    @NotBlank(message = "Phone cannot be blank")
    @Size(min = 10, max = 20, message = "Phone length should be between 10 and 20")
    private String phone;
}
