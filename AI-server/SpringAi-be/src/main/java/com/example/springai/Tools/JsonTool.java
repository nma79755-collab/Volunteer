package com.example.springai.Tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JsonTool {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 通用的 JSON 包装工具
     * 功能：把任何非 JSON 格式的工具返回值包装成标准的 JSON 对象，
     * 让 Sequential Thinking 等其他 MCP 工具能正常解析。
     *
     * 适用场景：
     * - 当 rag 工具返回纯文本"知识库没有相关案例"时
     * - 当任何工具返回的不是标准 JSON 时
     */
    @Tool(name = "wrap_as_json",
            description = """
              将任意工具的返回结果包装为标准 JSON 格式。
              
              使用时机：
              - 当某个工具返回了纯文本、中文消息等非 JSON 格式时
              - 需要把结果传递给其他需要标准 JSON 的工具时
              
              参数说明：
              - toolName: 原始工具的名称（如 "rag", "query_database"）
              - rawResult: 原始工具的返回结果（纯文本）
              """)
    public String wrapAsJson(
            @ToolParam(description = "原始工具的名称") String toolName,
            @ToolParam(description = "原始工具返回的纯文本结果") String rawResult) {

        try {
            Map<String, Object> wrapped = new HashMap<>();
            wrapped.put("tool", toolName);
            wrapped.put("status", "success");
            wrapped.put("data", rawResult);
            wrapped.put("isEmpty", rawResult == null || rawResult.isEmpty() ||
                    rawResult.contains("无结果") ||
                    rawResult.contains("没有") ||
                    rawResult.contains("0 hits"));

            String json = objectMapper.writeValueAsString(wrapped);
            log.debug("JSON 包装完成: {} -> {}", toolName, json);
            return json;
        } catch (Exception e) {
            log.error("JSON 包装失败: {}", e.getMessage());
            return String.format("{\"tool\":\"%s\",\"status\":\"error\",\"error\":\"%s\"}",
                    toolName, e.getMessage().replace("\"", "'"));
        }
    }
}