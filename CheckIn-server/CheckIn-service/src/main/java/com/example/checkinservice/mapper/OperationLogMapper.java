package com.example.checkinservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyh.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * OperationLog Mapper
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
    
    /**
     * Find operation logs by operator id
     * @param operatorId the operator id
     * @return list of operation logs
     */
    List<OperationLog> findByOperatorId(Long operatorId);
    
    /**
     * Find operation logs by operation type
     * @param operationType the operation type
     * @return list of operation logs
     */
    List<OperationLog> findByOperationType(String operationType);
    
    /**
     * Find operation logs by create time range
     * @param params map containing startTime and endTime
     * @return list of operation logs
     */
    List<OperationLog> findByCreateTime(Map<String, LocalDateTime> params);
}
