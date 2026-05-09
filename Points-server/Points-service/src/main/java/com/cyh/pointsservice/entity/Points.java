package com.cyh.pointsservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Points Entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_points")
public class Points {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    private Long activityId;
    private Long registrationId;
    private Integer pointsEarned;
    private String reason;
    private LocalDateTime createTime;
    private Integer isDeleted;
}
