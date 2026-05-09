package com.cyh.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Activity Entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_activity")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Activity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    private String location;
    private Integer requiredCount;
    private Integer registeredCount;
    private Integer basePoints;
    @TableField(exist = false)
    private List<String> imageUrl;
    private Integer status;
    private Long createdBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    private String updateBy;
    private Integer isDeleted;
    private String position;
    private Integer approved; // 0=待审核, 1=已审核通过
}
