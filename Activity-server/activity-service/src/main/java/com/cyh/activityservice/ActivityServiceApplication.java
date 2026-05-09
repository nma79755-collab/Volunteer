package com.cyh.activityservice;

import com.cyh.Config.FeignConfig;
import com.cyh.advice.CommonExceptionAdvice;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;

@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.cyh.Client",defaultConfiguration = FeignConfig.class)
@SpringBootApplication
@Import(CommonExceptionAdvice.class)
public class ActivityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActivityServiceApplication.class, args);
    }

}
