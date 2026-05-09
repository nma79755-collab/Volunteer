package com.example.springai.Tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class GetDateTool {

    /**
     * 获取当前日期和时间
     * 当用户询问"今天几号"、"现在几点"、"今天星期几"等问题时调用此工具
     */
    @Tool(name = "get_current_datetime",
            description = "获取当前精确的日期和时间。当用户询问今天是几号、星期几、当前时间时，必须调用此工具，不要凭记忆回答。")
    public String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE HH:mm:ss");
        return now.format(formatter);
    }

    /**
     * 获取当前日期（不含时间）
     */
    @Tool(name = "get_current_date",
            description = "获取当前日期。当用户只询问日期、今天是多少号时，使用此工具。")
    public String getCurrentDate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE");
        return now.format(formatter);
    }

    /**
     * 计算两个日期之间的天数差
     */
    @Tool(name = "calculate_date_diff",
            description = "计算从今天到指定日期还有多少天。当用户询问'还有几天到某日'、'距离某个日期还有多久'时使用。")
    public String calculateDateDiff(
            @ToolParam(description = "目标日期，格式为 yyyy-MM-dd，例如 2026-06-01") String targetDate) {
        try {
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
            LocalDateTime target = LocalDateTime.parse(targetDate + "T00:00:00");

            long days = java.time.Duration.between(now, target).toDays();

            if (days > 0) {
                return String.format("距离 %s 还有 %d 天", targetDate, days);
            } else if (days == 0) {
                return "就是今天！";
            } else {
                return String.format("%s 已经过去 %d 天了", targetDate, Math.abs(days));
            }
        } catch (Exception e) {
            return "日期格式错误，请使用 yyyy-MM-dd 格式，例如 2026-06-01";
        }
    }
}