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
 * User Entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sign;
    private String username;
    private String password;
    private String realName;
    private String email;
    private String phone;
    private Integer role;
    private Integer status;
    private Long totalPoints;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String avatar;
    private String updateBy;
    private Integer isDeleted;
    private Integer age;
    private Integer sex;
    private String city;
    private Integer checkcount;
    private Integer level;
}
