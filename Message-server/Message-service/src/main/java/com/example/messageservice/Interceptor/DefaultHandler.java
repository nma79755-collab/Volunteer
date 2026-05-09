package com.example.messageservice.Interceptor;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
//这里拿出userId并赋给 principal
@Component
public class DefaultHandler extends DefaultHandshakeHandler {
    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Long userId = (Long) attributes.get("userId");
        System.out.println("成功设置principal为"+userId);
        return () -> String.valueOf(userId);
    }
}
