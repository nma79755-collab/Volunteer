package com.example.springai.Tools;

import com.cyh.Utils.JwtUserDetails;
import com.cyh.Utils.UserContext;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class GetUserTool {
    //不用userContext获取, 因为工具调用不在一个线程中
    //如果要用,用InheritableThreadLocal,新起的线程会自动继承父线程的变量,如果从security上下文中获取也是一样,要把策略模式换成这个,这里使用toolContext
    @Tool(name = "get_current_user_Id",
            description = """
          获取当前登录用户ID。
          当用户问题中提到"我"、"我的"、"个人"、"当前用户"等关键词时使用此工具。
          例如：
          - "我的订单" → 先调用此工具获取用户ID
          - "我有哪些权限" → 先调用此工具获取用户ID
          - "查看我的个人资料" → 先调用此工具获取用户ID
          """)
    public String getCurrentUserInfo(ToolContext toolContext) {
        Long userId = (Long) toolContext.getContext().get("userId");
        System.out.println("获取到的userId为"+userId);
        if (userId == null) {
            return "错误：未获取到当前登录用户信息，请确认用户是否已登录";
        }
        return String.format("当前用户ID: %d",
               userId
        );
    }
}
