package com.example.springai.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "chat_memory")//指定集合名
public class MongoChatMessage {
    @Id  // ← 添加这个注解
    private String id;
    private String conversationId; //会话id,用于区别每次的对话
    private MessageType type; //消息类型
    private String content; //消息内容
    private LocalDateTime time;
    public static MongoChatMessage from(String conversationId, Message message){//添加时将springai封装的Message转化为mongo中需要的类型
        MongoChatMessage mongoChatMessage = new MongoChatMessage();
        mongoChatMessage.setConversationId(conversationId);
        mongoChatMessage.setContent(message.getText());
        mongoChatMessage.setTime(LocalDateTime.now());
        mongoChatMessage.setType(message.getMessageType());
        return mongoChatMessage;
    }
    public Message toMessage(){//转换为springai需要的Message类型
            return switch (type) { //枚举并转化成相应的类型
                case USER -> new UserMessage(content);
                case ASSISTANT -> new AssistantMessage(content);
                case SYSTEM -> new SystemMessage(content);
                default -> throw new IllegalArgumentException("不支持的消息类型: " + type);
            };
        }
}
