package com.example.springai.Config;

import com.example.springai.Repository.MongoRepository;
import com.example.springai.Tools.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Slf4j
@Configuration
public class AiConfig {
    @Autowired
    private RagTool ragTool;
    @Autowired
    private Nl2SqlTool nl2SqlTool;
    @Autowired
    private JsonTool jsonTool;
    @Autowired
    private GetDateTool getDateTool;
    @Autowired
    private GetUserTool getUserTool;
    @Autowired
    private ToolCallbackProvider toolCallbackProvider;
    @Bean//这个用于用户聊天
    @Primary //标记为主bean,默认注入
    public ChatClient chatClient(DeepSeekChatModel deepSeekChatModel,MongoRepository mongoRepository){//返回ChatClient,注入配置好的会话记忆仓库
        return ChatClient.builder(deepSeekChatModel)//选定默认模型
                .defaultTools( ragTool, nl2SqlTool,jsonTool,getDateTool, getUserTool)//添加工具,调用后获取的内容自动封装为系统提示词，不需要手动处理提示词了
                .defaultToolCallbacks(toolCallbackProvider.getToolCallbacks())//添加工具回调,自动调用所有的工具,包括mcp
                .defaultSystem("你是佩奇")//默认提示词
                .defaultAdvisors(new SimpleLoggerAdvisor(),//开启日志
                        MessageChatMemoryAdvisor.builder(mongoRepository).build()//指定会话记忆仓库
                )
                .build();
    }
    // 专门用于文档分类的 ChatClient（新模型、无记忆、无日志）
    @Bean
    public ChatClient classifyChatClient(DeepSeekChatModel deepSeekChatModel) {
        return ChatClient.builder(deepSeekChatModel)
                .defaultSystem("你是一个文档分类专家")  // 专门的系统提示
                .build();
    }
    @Bean
    public ChatClient DeepThinkChatClient(DeepSeekChatModel deepSeekChatModel) {
        return ChatClient.builder(deepSeekChatModel)
                .defaultSystem("""
            你是一个思维缜密的分析专家。请对用户的问题进行真正的深度思考。
            
            **思考要求：**
            1. 不要急于给出答案，而是先剖析问题本身
            2. 从多个对立或互补的角度审视问题
            3. 识别你的知识边界，明确哪些是需要查证的
            4. 如果问题有隐含的前提假设，指出来并评估其合理性
            5. 思考可能的答案范围，而不是单一结论
            
            **思考框架：**
            > 💡 **第1步：理解问题的核心**
            > - 用户真正想问的是什么？
            > - 这个问题背后有什么隐含需求？
            > - 问题有没有模糊或歧义的地方？
            
            > 📊 **第2步：多角度拆解**
            > - 从哪些维度来思考这个问题？
            > - 每个维度需要考虑哪些因素？
            > - 不同维度之间有什么关联或矛盾？
            
            > 🔍 **第3步：识别知识缺口**
            > - 我的哪些知识是可靠的？哪些是推测的？
            > - 有什么关键信息是我目前不具备的？
            > - 如果信息不足，我需要查什么类型的资料？
            
            > 🧠 **第4步：探索多种可能性**
            > - 不要只考虑一种答案，列出至少两种可能的方向
            > - 每种方向的支持理由和反对理由分别是什么？
            > - 有没有第三种或第四种可能性？
            
            > ⚠️ **第5步：评估不确定性和风险**
            > - 我的分析中哪些部分是不确定的？
            > - 如果我的判断有偏差，可能偏在哪里？
            > - 我应该用什么措辞来表达不确定性？
            
            > ✅ **第6步：规划回答结构**
            > - 最终回答应该包括哪几个部分？
            > - 每个部分的核心要点是什么？
            > - 如何让回答既有深度又易于理解？
            
            **重要：** 这只是思考过程，不要给出最终答案。思考完成后用 "思考完毕，以下是正式回答：" 收尾。
            """+"**严格禁令（必须遵守）：**\n" +
                        "1. 你只负责思考，绝对不要写最终答案\n" +
                        "2. 不要写任何代码示例、教程、指南\n" +
                        "3. 不要使用'以下是回答'、'总结如下'、'下面我来'等过渡语\n" +
                        "4. 你的角色是'思考者'，不是'回答者'\n" +
                        "5. 如果发现自己开始写答案了，立刻停止，用'思考完毕。'结束\n" +
                        "6. 你的输出长度不应该超过500字，超过说明你写答案了\n"+
                        "7. 不要调用任何工具、搜索、数据库查询\n" +
                        "8. 只基于你自己的推理能力和知识进行思考\n" )  // 专门的系统提示
                .build();
    }
}
