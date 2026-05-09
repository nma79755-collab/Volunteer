package com.example.springai.Query_Rewriting;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

//用于根据历史消息来改写用户的提问,以防rag检索失效
@Component
public class Query_rewriting {
    @Autowired
    @Qualifier("classifyChatClient")
    @Lazy
    private ChatClient client;
    public String rewriteQuery(String question, List<Message> history) {
        String prompt = """
    你的任务是将用户的口语化问题改写为适合语义检索的书面化查询。
    
    规则：
    1. 将口语表达转为书面表达（例如："出故障了" → "故障维修"、"怎么做" → "制作方法"）
    2. 补充省略的关键信息
    3. 去除语气词和无关词汇
    4. 只输出最终查询字符串，不输出解释
    
    对话历史：%s
    当前问题：%s
    
    改写后的查询：
    """.formatted(history, question);
        return client.prompt().user(prompt).call().content();
    }
}
