package com.example.messageservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyh.entity.ChatMessage;
import com.cyh.entity.Message;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

/**
 * User Mapper
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
    int insertChatMessage(String content, Long senderId, Long receiverId, LocalDateTime sendTime, Integer status, Integer type);
}
