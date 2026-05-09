package com.example.springai.Controller;

import com.cyh.Utils.UserContext;
import com.example.springai.Compressor.CompressHandler;
import com.example.springai.Entity.ChatMessage;
import com.example.springai.Tools.Nl2SqlTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai")
public class ChatController {
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private ChatClient client;
    @Autowired
    private Nl2SqlTool nl2SqlTool;
    @Autowired
    @Qualifier("DeepThinkChatClient")
    private ChatClient DeepSeekClient;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CompressHandler compressHandler;

    @PostMapping(value = "/chat",produces = "text/event-stream;charset=utf-8")//保证流式输出编码问题
    public Flux<String> Chat(@RequestParam("message") String message, @RequestParam("conversationId") String conversationId) {
        compressHandler.compressHistory(conversationId);
        Long userId = UserContext.getUser();
        Flux<String> content = client.prompt()
                .system("你是志愿者管理系统的智能助手，你的名字叫佩奇。\n\n" +

                        "**核心工作流程：**\n" +
                        "1. 当用户询问个人信息、志愿者信息等需要从数据库查询的问题时，优先使用 query_database 工具。\n" +
                        "2. 如果数据库查询返回空结果或失败，必须调用 rag 工具从知识库检索，不要凭记忆回答。\n" +
                        "3. 当用户询问日期、时间、星期时，必须调用 get_current_datetime 工具获取实时信息。\n" +
                        "4. 当用户询问产品价格、实时新闻、网络资讯等外部信息时，必须优先调用联网搜索工具。\n" +
                        "\n" +

                        "**思考决策规则：**\n" +
                        "5. 当用户要求对观点进行利弊分析、辩论或结构化审视时，使用 structured_argumentation 工具。\n" +
                        "6. 当问题涉及多步骤推理、方案策划、复杂分析时，优先调用 sequential-thinking 工具分步思考。\n" +
                        "7. 如果 sequential-thinking 调用其他工具后返回非 JSON 格式的结果，先用 jsontool 包装成 JSON。\n" +
                        "\n" +

                        "**重要限制：**\n" +
                        "8. 数据库查询结果为空时，告知用户结果为空，不要联网搜索。\n" +
                        "9. 查询失败时，告知用户暂时查不到，建议换个问法。\n" +
                        "10. 用友好、口语化的方式回答用户。\n" +
                        "\n" +

                        "**Token 监控规则（必须执行）：**\n" +
                        "11. 每轮回答结束后检查对话轮次，超过 10 轮时必须调用 compress_history 工具压缩历史。\n" +
                        "12. 压缩完成后告知用户：'对话历史已自动压缩，关键信息已保留。'"+
                        "**验证规则（重要）：**\n" +
                        "1. 当用户质疑你的回答（如提问'你确定吗？'、'再查一遍'、'有没有调工具'、'让我亲眼看到'），\n" +
                        "   必须重新调用工具获取最新数据，不要使用之前的缓存结果。\n" +
                        "2. 重新调用工具后，在回答中说明：'刚刚重新查询了一次，结果如下：'\n" +
                        "3. 如果两次查询结果一致，可以回答'再次确认，信息无误'。\n" +
                        "4. 即使你100%确定答案，只要用户要求'重新查'、'再查一遍'，也必须重新调用工具。")
                .user(message)//用户消息
                .advisors(advisorSpec -> advisorSpec.param("chat_memory_conversation_id", conversationId))//开启会话记忆,参数名必须是这个才能正确传递,让Message封装对话Id并传给仓库
                .toolContext(Map.of("userId", userId)) //将用户Id封装在toolContext中
                // .call()//发送消息
                .stream()//流式输出
                .content()
                .onErrorResume(err -> {
                    log.error("对话出错: {}", err.getMessage());
                    return Flux.just("抱歉，处理你的问题时出了点小状况，换个问法试试？😅");
                }
                );
        return content;
    }
    @GetMapping("/getAllConversations")
    public List<String> getAllConversations() {
        String query_for_conversation = "SELECT conversationId from conversation where userId = ? order by createTime desc ";
        List<String> conversations = jdbcTemplate.queryForList(query_for_conversation,String.class,1);
        if(conversations.isEmpty()){
            return null;
        }
        return conversations;
    }
    @PostMapping("/newConversation")
    public void newConversation(@RequestParam("conversationId") String conversationId) {
        String insert_conversation = "INSERT INTO conversation (conversationId,userId,createTime) VALUES (?,1,?)";
        jdbcTemplate.update(insert_conversation,conversationId, LocalDateTime.now());
    }

    @GetMapping("/getHistory")
    public List<ChatMessage> getHistory(@RequestParam("conversationId") String conversationId) {
        Query query = new Query(Criteria.where("conversationId").is(conversationId))
                .with(Sort.by(Sort.Direction.DESC, "time"));
        return mongoTemplate.find(query, ChatMessage.class);
    }

    @GetMapping("/test/query/{message}")
    public String testQuery(@PathVariable("message") String question) {
        return nl2SqlTool.query(question);
    }

    @PostMapping(value = "/deepThink",produces = "text/event-stream;charset=utf-8")
    public Flux<String> DeepSeekChat(@RequestParam("message") String message,
                                     @RequestParam("conversationId") String conversationId) {
        compressHandler.compressHistory(conversationId);
        Long userId = UserContext.getUser();
        Flux<String> thinkingStream = DeepSeekClient.prompt()
                .user(message)
                .stream()
                .content();

        return Flux.defer(() -> {
            StringBuilder fullThinking = new StringBuilder();

            return thinkingStream
                    .startWith("【深度思考过程】\n\n")
                    .doOnNext(fullThinking::append)
                    .concatWith(Flux.defer(() -> {
                        String thinkingResult = fullThinking.toString();
                        Flux<String> endMarker =Flux.just("\n\n【结束思考，开始正式回答】\n\n");
                        Flux<String> peppaResponse = client.prompt()
                                .system("你是志愿者管理系统的智能助手，你的名字叫佩奇。\n\n" +

                                        "**核心工作流程：**\n" +
                                        "1. 当用户询问个人信息、志愿者信息等需要从数据库查询的问题时，优先使用 query_database 工具。\n" +
                                        "2. 如果数据库查询返回空结果或失败，必须调用 rag 工具从知识库检索，不要凭记忆回答。\n" +
                                        "3. 当用户询问日期、时间、星期时，必须调用 get_current_datetime 工具获取实时信息。\n" +
                                        "4. 当用户询问产品价格、实时新闻、网络资讯等外部信息时，必须优先调用联网搜索工具。\n" +
                                        "\n" +

                                        "**思考决策规则：**\n" +
                                        "5. 当用户要求对观点进行利弊分析、辩论或结构化审视时，使用 structured_argumentation 工具。\n" +
                                        "6. 当问题涉及多步骤推理、方案策划、复杂分析时，优先调用 sequential-thinking 工具分步思考。\n" +
                                        "7. 如果 sequential-thinking 调用其他工具后返回非 JSON 格式的结果，先用 jsontool 包装成 JSON。\n" +
                                        "\n" +

                                        "**重要限制：**\n" +
                                        "8. 数据库查询结果为空时，告知用户结果为空，不要联网搜索。\n" +
                                        "9. 查询失败时，告知用户暂时查不到，建议换个问法。\n" +
                                        "10. 用友好、口语化的方式回答用户。\n" +
                                        "\n" +

                                        "**Token 监控规则（必须执行）：**\n" +
                                        "11. 每轮回答结束后检查对话轮次，超过 10 轮时必须调用 compress_history 工具压缩历史。\n" +
                                        "12. 压缩完成后告知用户：'对话历史已自动压缩，关键信息已保留。'"+
                                        "**验证规则（重要）：**\n" +
                                                "1. 当用户质疑你的回答（如提问'你确定吗？'、'再查一遍'、'有没有调工具'、'让我亲眼看到'），\n" +
                                                "   必须重新调用工具获取最新数据，不要使用之前的缓存结果。\n" +
                                                "2. 重新调用工具后，在回答中说明：'刚刚重新查询了一次，结果如下：'\n" +
                                                "3. 如果两次查询结果一致，可以回答'再次确认，信息无误'。\n" +
                                                "4. 即使你100%确定答案，只要用户要求'重新查'、'再查一遍'，也必须重新调用工具。")
                                .user(message)
                                .advisors(advisorSpec -> advisorSpec.param(
                                        "chat_memory_conversation_id", conversationId))
                                .toolContext(Map.of("userId", userId))
                                .stream()
                                .content();

                        return Flux.concat(endMarker, peppaResponse);
                    }))
                    .onErrorResume(err -> {
                        log.error("深度思考出错: {}", err);
                        return Flux.just(
                                "\n\n抱歉，思考过程中出了点小状况，换个问法试试？😅"
                        );
                    });
        });
    }
}
