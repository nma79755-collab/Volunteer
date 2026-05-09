package com.cyh.reviewservice.Dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Review Batch Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewBatchRequest {

    /**
     * List of registration IDs to review
     */
    @NotEmpty(message = "报名ID列表不能为空")
    private List<Long> registrationIds;

    /**
     * Action: "approve" or "reject"
     */
    @NotBlank(message = "操作类型不能为空")
    @Pattern(regexp = "APPROVE|REJECT", message = "操作类型只能是approve或reject")
    private String action;

    /**
     * Reason (required when action is "reject")
     */
    private String reason;
}
