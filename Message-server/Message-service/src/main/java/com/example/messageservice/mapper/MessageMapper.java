package com.example.messageservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyh.entity.Message;
import com.cyh.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * User Mapper
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

  int insertMessage(String content, Long sender, String receiver, LocalDateTime sendTime, Integer status);
}
