package com.cyh.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Change Password Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {
    
    /**
     * Old password
     */
    @NotBlank(message = "Old password cannot be blank")
    private String oldPassword;
    
    /**
     * New password
     */
    @NotBlank(message = "New password cannot be blank")
    @Size(min = 6, max = 100, message = "New password length should be between 6 and 100")
    private String newPassword;
    
    /**
     * Confirm password
     */
    @NotBlank(message = "Confirm password cannot be blank")
    private String confirmPassword;
}
