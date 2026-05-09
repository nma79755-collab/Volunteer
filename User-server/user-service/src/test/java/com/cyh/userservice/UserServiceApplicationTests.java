package com.cyh.userservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@SpringBootTest
class UserServiceApplicationTests {
@Autowired
private SimpMessagingTemplate messagingTemplate;
    @Test
    void contextLoads() {
        messagingTemplate.convertAndSend("/queue/all-reviews", 1);//广播,前端筛选通知
    }

}
