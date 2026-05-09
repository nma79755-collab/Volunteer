package com.example.checkinservice.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Check-in Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInRequest {

    /**
     * Registration ID for the check-in
     */
    @NotNull(message = "报名ID不能为空")
    private Long registrationId;
}
