package com.cyh.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Registration Entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_registration")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Registration {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    private Long activityId;
    private Integer status;
    @TableField("reject_reason")
    private String rejectReason;
    private Long reviewedBy;
    private LocalDateTime reviewTime;
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
    private String updateBy;
    @TableField("is_deleted")
    private Integer isDeleted;
}
