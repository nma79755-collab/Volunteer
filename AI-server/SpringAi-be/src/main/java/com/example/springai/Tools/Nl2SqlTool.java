package com.example.springai.Tools;

import com.example.springai.DatabaseSchema.DatabaseSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
@Slf4j
@Component
public class Nl2SqlTool {
    @Autowired
    private DatabaseSchema  databaseSchema;
    @Autowired
    @Lazy
    private ChatClient client;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Tool(name = "query_database",
            description = "将自然语言问题转换为SQL并查询MySQL数据库。\n" +
                    "适用场景：查询志愿者活动、统计报名人数、筛选活动类型、查询用户信息等数据库查询问题。\n" +
                    "当用户问需要从数据库中获取数据时，调用此工具。")
    public String query(@ToolParam String question) {
        try {
            String fullSchemaDescription = databaseSchema.getFullSchemaDescription();

            String prompt = buildPrompt(fullSchemaDescription, question);

            String sql = generateSql(prompt);
            if (sql.contains("无法生成SQL")) {
                return "查询结果为空。";  // 返回干净的空结果
            }

            List<Map<String, Object>> maps = jdbcTemplate.queryForList(sql);

            String res = formatResult(maps, sql);

            return res;

        } catch (Exception e) {
            log.error("查询失败，错误详情：", e);
            return List.of().toString(); // 返回空列表
        }
    }
    /**
     * 构建 Prompt
     */
    private String buildPrompt(String schema, String question) {
        return String.format("""
            你是一个专业的SQL查询助手。根据下面的数据库表结构，将用户的自然语言问题转换为SQL查询语句。
            
            【数据库表结构】
            %s
            
            【用户问题】
            %s
            
            【要求】
            1. 只返回SQL语句，不要有任何解释或其他文字
            2. SQL语句以分号结尾
            3. 只生成SELECT查询语句
            4. 如果问题无法转换为SQL，返回：无法生成SQL
            
            【SQL语句】
            """, schema, question);
    }
    /**
     * 调用 DeepSeek 生成 SQL
     */
    private String generateSql(String prompt) {
        String sql = client.prompt()
                .user(prompt)
                .call()
                .content();

        // 清理 SQL（去除可能的 Markdown 代码块标记）
        sql = sql.trim()
                .replaceAll("```sql\\n?", "")
                .replaceAll("```\\n?", "")
                .replaceAll("\\n$", "");

        return sql;
    }
    /**
     * 安全校验：只允许 SELECT 语句
     */
    private boolean isSafeSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }

        String lowerSql = sql.trim().toLowerCase();

        // 只允许 SELECT 开头
        if (!lowerSql.startsWith("select")) {
            return false;
        }

        // 禁止危险操作
        String[] dangerous = {"drop", "delete", "update", "insert", "alter",
                "create", "truncate", "grant", "revoke"};
        for (String keyword : dangerous) {
            if (lowerSql.contains(keyword)) {
                return false;
            }
        }

        return true;
    }
    /**
     * 格式化返回结果
     */
    private String formatResult(List<Map<String, Object>> result, String sql) {
        if (result == null || result.isEmpty()) {
            return "查询结果为空。\n\n生成的SQL: " + sql;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("查询成功！共返回 ").append(result.size()).append(" 条记录。\n\n");

        // 限制最多显示 20 条
        int limit = Math.min(result.size(), 20);//取前 20 条或总数

        for (int i = 0; i < limit; i++) {
            sb.append("【记录 ").append(i + 1).append("】\n");
            Map<String, Object> row = result.get(i);
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            sb.append("\n");
        }

        if (result.size() > 20) {
            sb.append("... 仅显示前20条，共 ").append(result.size()).append(" 条\n\n");
        }

        sb.append("生成的SQL: ").append(sql);

        return sb.toString();
    }
}
