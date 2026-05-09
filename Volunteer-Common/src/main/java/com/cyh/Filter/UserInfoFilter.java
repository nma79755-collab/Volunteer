package com.cyh.Filter;

import com.cyh.Utils.JwtUserDetails;
import com.cyh.Utils.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

//这是对每个微服务而言的前置过滤器,包括网关和其他微服务的请求
public class UserInfoFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        System.out.println("拦截器成功生效");
        String userInfo = request.getHeader("userInfo");
        if (userInfo == null || userInfo.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        // JSON 字符串转回对象
        JwtUserDetails details = objectMapper.readValue(userInfo, JwtUserDetails.class);
        String userId = details.getUserId().toString();
        String username = details.getUsername();
        String role = details.getRole();
        System.out.println("获取到的userID为"+userId);
        ArrayList<SimpleGrantedAuthority> authorities = new ArrayList<>();
        String normalizedRole = "VOLUNTEER";
        if(role!=null){
            if ("ADMIN".equals(role) || "1".equals(role)) {
                normalizedRole = "ADMIN";
            }
            authorities.add(new SimpleGrantedAuthority("ROLE_" + normalizedRole));
        }
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(username, null, authorities);//创建认证对象
        usernamePasswordAuthenticationToken.setDetails(details); //将整个权限对象封装在detail
        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken); //存储上下文
        if(!userId.isEmpty()){
            UserContext.setUser(Long.valueOf(userId));
        }
        filterChain.doFilter(request, response);
    }
}
