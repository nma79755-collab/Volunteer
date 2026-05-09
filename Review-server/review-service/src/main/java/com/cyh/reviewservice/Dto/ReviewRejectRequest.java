package com.cyh.reviewservice.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Review Reject Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRejectRequest {

    /**
     * Reason for rejection
     */
    @NotBlank(message = "拒绝原因不能为空")
    @Size(max = 500, message = "拒绝原因不能超过500个字符")
    private String rejectReason;
}
