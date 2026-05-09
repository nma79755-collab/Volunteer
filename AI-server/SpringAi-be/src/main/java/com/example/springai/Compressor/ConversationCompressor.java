package com.example.springai.Compressor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ConversationCompressor {
    @Autowired
    @Lazy
    private ChatClient chatClient;
    private static final int MAX_TOKENS = 4096;
    // 保留最近 N 条消息不被压缩
    private static final int MAX_MESSAGES = 10;
    public boolean isCompressed(List<Message> conversation) {
        int tokens = estimateTokens(conversation);
        return tokens > MAX_TOKENS && conversation.size() > MAX_MESSAGES;//当消息大于10条而且token数大于4096则进行压缩
    }
    public List<Message> compress(List<Message> messages) {
        if (!isCompressed(messages)) {
            return messages;
        }
        // 1. 分离"旧消息"和"最近消息"
        List<Message> oldMessages = new ArrayList<>(messages.subList(0, messages.size() - MAX_MESSAGES)); //保留最新消息不被压缩
        List<Message> recentMessages = new ArrayList<>(messages.subList(messages.size() - MAX_MESSAGES, messages.size()));
        // 2. 提取旧消息中的"摘要"（如果之前已经压缩过）
        String existingSummary = extractExistingSummary(oldMessages);
        //将之前的总结和旧消息再次总结
        String summarize = summarize(oldMessages, existingSummary);
        ArrayList<Message> messagesList = new ArrayList<>();
        messagesList.add(new SystemMessage("【对话历史摘要】" + summarize));//将总结写入系统消息
        messagesList.addAll(recentMessages);
        log.info("对话压缩完成：原始 {} 条消息 → 压缩后 {} 条", messages.size(), messagesList.size());
        return messagesList;
    }
    /**
     * 提取已有的摘要（如果之前压缩过）
     */
    private String extractExistingSummary(List<Message> messages) {
        for (Message msg : messages) {
            if (msg instanceof SystemMessage) {
                String text = ((SystemMessage) msg).getText();
                if (text != null && text.startsWith("【对话历史摘要】")) {
                    return text.substring("【对话历史摘要】".length()).trim();
                }
            }
        }
        return "";
    }
    /**
     * 调用 AI 生成摘要
     */
    private String summarize(List<Message> messages, String existingSummary) {
        // 把消息列表转成可读文本
        String conversationText = messages.stream()
                .filter(msg -> msg instanceof UserMessage || msg instanceof AssistantMessage)
                .map(msg -> {
                    String role = msg instanceof UserMessage ? "用户" : "助手";
                    String content = msg instanceof UserMessage ?
                            ((UserMessage) msg).getText() :
                            ((AssistantMessage) msg).getText();
                    return role + "：" + (content != null ? content.substring(0, Math.min(content.length(), 200)) : "");
                })
                .collect(Collectors.joining("\n"));

        // 构建压缩提示词
        String prompt = String.format("""
                请用一段话（不超过300字）总结以下对话的核心内容，重点保留：
                1. 用户的关键需求和问题
                2. 已经完成的查询和操作
                3. 得到的结论和待解决的问题
                
                %s
                
                %s
                
                请直接输出总结，不要加任何前缀说明。
                """,
                existingSummary.isEmpty() ? "" : "此前的对话摘要：" + existingSummary + "\n\n以下是新的对话内容：",
                conversationText
        );

        try {
            String result = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            log.info("对话摘要生成成功，长度: {} 字", result != null ? result.length() : 0);
            return result != null ? result : "对话内容较多，涉及多个话题。";
        } catch (Exception e) {
            log.error("生成对话摘要失败: {}", e.getMessage());
            return "对话内容较多，涉及多个话题。";
        }
    }
    /**
     * 估算 Token 数（简单估算：中文约 2 字符/Token，英文约 4 字符/Token）
     */
    private int estimateTokens(List<Message> messages) {
        int totalChars = messages.stream()
                .mapToInt(msg -> {
                    if (msg instanceof UserMessage) {
                        String text = ((UserMessage) msg).getText();
                        return text != null ? text.length() : 0;
                    } else if (msg instanceof AssistantMessage) {
                        String text = ((AssistantMessage) msg).getText();
                        return text != null ? text.length() : 0;
                    } else if (msg instanceof SystemMessage) {
                        String text = ((SystemMessage) msg).getText();
                        return text != null ? text.length() : 0;
                    }
                    return 0;
                })
                .sum();
        return totalChars / 2;  // 中文占多数，按 2 字符/Token 估算
    }
}
