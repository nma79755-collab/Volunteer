package com.cyh.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * OperationLog Entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long operatorId;
    private String operationType;
    private String operationObject;
    private String operationContent;
    private Integer operationResult;
    private String errorMessage;
    private LocalDateTime createTime;
    private Integer isDeleted;
}
