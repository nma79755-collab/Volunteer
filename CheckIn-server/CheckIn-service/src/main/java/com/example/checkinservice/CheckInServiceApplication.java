package com.example.checkinservice;

import com.cyh.Config.FeignConfig;
import com.cyh.advice.CommonExceptionAdvice;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;

@EnableFeignClients(basePackages = "com.cyh.Client",defaultConfiguration = FeignConfig.class)
@EnableDiscoveryClient
@Import(CommonExceptionAdvice.class)
@SpringBootApplication
public class CheckInServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CheckInServiceApplication.class, args);
    }

}
