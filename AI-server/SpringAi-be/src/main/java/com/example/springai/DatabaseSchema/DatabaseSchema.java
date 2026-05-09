package com.example.springai.DatabaseSchema;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DatabaseSchema {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    public List<String> getTableNames() {
        String sql = "SHOW TABLES";
        return jdbcTemplate.queryForList(sql, String.class);
    }//获取数据库中所有的表名
    public List<Map<String, Object>> getTableColumns(String tableName) {
        String sql = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT, IS_NULLABLE, COLUMN_KEY " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
        return jdbcTemplate.queryForList(sql, tableName);
    }//获取指定表的所有列
    /**
     * 获取完整的数据库 Schema 描述（用于 Prompt）
     */
    public String getFullSchemaDescription() {
        StringBuilder schema = new StringBuilder();
        List<String> tables = getTableNames();

        for (String table : tables) {
            schema.append("表名: ").append(table).append("\n");
            schema.append("字段:\n");

            List<Map<String, Object>> columns = getTableColumns(table);
            for (Map<String, Object> col : columns) {
                String colName = (String) col.get("COLUMN_NAME");
                String dataType = (String) col.get("DATA_TYPE");
                String comment = (String) col.get("COLUMN_COMMENT");
                String isKey = "PRI".equals(col.get("COLUMN_KEY")) ? " (主键)" : "";

                schema.append("  - ")
                        .append(colName).append(" (").append(dataType).append(")")
                        .append(isKey)
                        .append(comment != null && !comment.isEmpty() ? " : " + comment : "")
                        .append("\n");
            }
            schema.append("\n");
        }
        return schema.toString();
    }//获取完整的数据库 Schema 描述（用于 Prompt）
}
