package com.example.springai.Tools;

import com.example.springai.KeySearch.RagByKey;
import com.example.springai.Query_Rewriting.Query_rewriting;
import com.example.springai.Repository.MongoRepository;
import com.example.springai.Service.RagService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RagTool {
    @Autowired
    private Query_rewriting queryRewriting;
    @Autowired
    private MongoRepository mongoRepository;
    @Autowired
    private RagService ragService;
    @Autowired
    private VectorStore vectorStore;
    @Autowired
    private RagByKey ragByKey;
    @Tool(name = "rag",description = "搜索内部知识库，获取与用户问题相关的文档内容。\n" +
            "          适用场景：食谱做法、设备维修、操作指南、故障处理、人物查询、名词解释等需要查询知识库的问题。\n" +
            "          当你不确定答案时，优先调用此工具搜索知识库。")//封装为tool以调用
    public String searchKnowledgeBase(@ToolParam(description = "用户当前的问题或查询内容") String question,@ToolParam(description = "当前会话ID，用于获取对话历史") String conversationId) throws IOException, ParseException {
        List<Message> messages = mongoRepository.get(conversationId);
        String rewriteQuery = queryRewriting.rewriteQuery(question, messages);
        System.out.println("改写后的问题为"+rewriteQuery);
        String s = ragService.askByRag(rewriteQuery);
        System.out.println("返回的文档为"+s);
        if(s.isEmpty()){
            System.out.println("rag检索无结果，开始BM检索");
            ragByKey.initIndex();
            s = ragByKey.getDocByKey(rewriteQuery, 2);
            System.out.println("BM检索回的文档为"+s);
        }
        if(s.isEmpty()){
            System.out.println("===============改写问题返回空结果，尝试使用原问题检索=================");
             s = ragService.askByRag(rewriteQuery);
            System.out.println("原问题返回的文档为"+s);
            if(s.isEmpty()){
                System.out.println("rag检索无结果，开始BM检索");
                s = ragByKey.getDocByKey(rewriteQuery, 2);
                System.out.println("BM检索回的文档为"+s);
            }
        }
        return s;
    }
}
