package com.example.registrationservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Registration List Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationListResponse {
    
    private long total;
    private int pageNum;
    private int pageSize;
    private List<RegistrationResponse> records;
}
