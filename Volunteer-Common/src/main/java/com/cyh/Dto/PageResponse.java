package com.cyh.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Pagination Response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private long total;
    private int pageNum;
    private int pageSize;
    private List<T> records;
}
