package com.cyh.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.List;

@Data
public class ActivityImg {
    private Long id;
    @TableField("url")
    private String url;
    @TableField("activityId")
    private Long activityId;
}
