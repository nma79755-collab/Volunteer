package com.cyh.Config;

import com.cyh.Utils.JwtUserDetails;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.context.SecurityContextHolder;


public class FeignConfig {
    @Bean
    public Logger.Level feignLogger(){
        return Logger.Level.FULL;//开启feign全日志记录
    }
    @Bean
    public RequestInterceptor requestInterceptor(){
      return   RequestTemplate ->{
          JwtUserDetails userDetails = (JwtUserDetails) SecurityContextHolder.getContext().getAuthentication().getDetails();
          ObjectMapper objectMapper = new ObjectMapper();
          try {
              objectMapper.writeValueAsString(userDetails);
              RequestTemplate.header("userInfo",objectMapper.writeValueAsString(userDetails));
          } catch (JsonProcessingException e) {
              throw new RuntimeException(e);
          }
      };
    }
}
