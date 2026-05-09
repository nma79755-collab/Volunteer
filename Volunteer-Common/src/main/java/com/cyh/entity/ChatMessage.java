package com.cyh.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_chatMessage")
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String content;
    private Integer type;
    @TableField("senderId")
    private Long senderId;
    @TableField("receiverId")
    private Long receiverId;
    @TableField("senderName")
    private String senderName;
    @TableField("receiverName")
    private String receiverName;
    @TableField("sendTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime sendTime;
    private Integer status;
    @TableField("senderAvatar")
    private String senderAvatar;
    @TableField("receiverAvatar")
    private String receiverAvatar;
}
