package com.cyh.gatewayservice.Filter;

import com.cyh.gatewayservice.Utils.JwtTokenProvider;
import com.cyh.gatewayservice.Utils.JwtUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
@Slf4j
@Component
public class GlobalGateWayFilter implements GlobalFilter, Ordered {
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private ObjectMapper objectMapper;
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath().trim();
        if (path.equals("/api/auth/login")
                || path.equals("/api/auth/register")
                || path.equals("/error")
                || path.startsWith("/ws")
                || path.equals("/health")
                || path.startsWith("/api/test")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")) {
            System.out.println("jwt放行");
            return chain.filter(exchange);
        }
        try{
        List<String> strings = exchange.getRequest().getHeaders().get("Authorization");

        String token = null;
        if (strings != null && strings.size() > 0) {
             token = strings.get(0).substring(7).trim();
             System.out.println("token:"+token);
        }
        if (token != null && jwtTokenProvider.validateToken(token)) {
            System.out.println("token正常");
            Claims claims = jwtTokenProvider.getClaimsFromToken(token);
            System.out.println("claims"+claims);
            if (claims != null) {
                Long userId = jwtTokenProvider.extractUserId(token);
                String username = jwtTokenProvider.extractUsername(token);
                String role = jwtTokenProvider.extractRole(token);
                JwtUserDetails jwtUserDetails = new JwtUserDetails(userId, username, role);//将用户信息封装成JwtUserDetails
                String s = objectMapper.writeValueAsString(jwtUserDetails); //序列化
                System.out.println("s的值为"+s);
                ServerWebExchange userInfo = exchange.mutate().request(builder -> builder.header("userInfo", s)).build(); //把用户信息写入请求头,传递给下游
                return chain.filter(userInfo);
            }
        }
    } catch (Exception e) {
        log.error("Failed to set user authentication: {}", e.getMessage());
    }
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete(); //未登录不放行
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
