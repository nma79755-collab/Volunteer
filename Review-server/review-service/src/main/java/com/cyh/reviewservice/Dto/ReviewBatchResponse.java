package com.cyh.reviewservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Review Batch Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewBatchResponse {

    private int successCount;
    private int failureCount;
}
