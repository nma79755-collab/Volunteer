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
 * CheckIn Entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_check_in")
public class CheckIn {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long registrationId;
    private Long userId;
    private Long activityId;
    private Integer status;
    private LocalDateTime checkInTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}
