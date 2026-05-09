package com.example.springai.Entity;

import lombok.Data;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document("chat_history")
public class ChatMessage {
    @Id
    private String id;
    private String conversationId;  // 会话ID
    private String userId;          // 用户ID（如果有用户系统）
    private String userMessage;     // 用户问题
    private String aiResponse;      // AI完整回答
    private LocalDateTime createTime;
    public static ChatMessage chatMessage(String conversationId,Message message){//将Message类型转化为存储历史的类型
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setConversationId(conversationId);
        MessageType messageType = message.getMessageType();
        if(messageType==MessageType.USER) {
            chatMessage.setUserMessage(message.getText());
        }
        if(messageType==MessageType.ASSISTANT) {
            chatMessage.setAiResponse(message.getText());
        }
        chatMessage.setCreateTime(LocalDateTime.now());
        return chatMessage;
    }
}
