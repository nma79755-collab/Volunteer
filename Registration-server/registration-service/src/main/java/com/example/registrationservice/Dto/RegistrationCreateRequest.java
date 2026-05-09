package com.example.registrationservice.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Registration Create Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationCreateRequest {

    /**
     * Activity ID to register for
     */
    @NotNull(message = "活动ID不能为空")
    private Long activityId;
}
