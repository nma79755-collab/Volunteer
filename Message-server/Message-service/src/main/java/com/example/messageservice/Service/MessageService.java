package com.example.messageservice.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cyh.Client.UserClient;
import com.cyh.entity.Message;
import com.cyh.entity.User;
import com.example.messageservice.mapper.MessageMapper;
import org.checkerframework.checker.units.qual.A;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageService {
    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private SimpMessagingTemplate messagingTemplate; //websocket消息推送对象
    @Autowired
    private UserClient userClient;
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "directQueueForReview",durable = "true") ,
            exchange = @Exchange(name = "directExchangeForReview",type = ExchangeTypes.DIRECT),
            key ={"review"}
    ))
    public void getMessage(Message message) {
        message.setStatus(0);
        messageMapper.insertMessage(message.getContent(),message.getSender(),message.getReceiver(),message.getSendTime(),message.getStatus());
        System.out.println("发送消息到: /queue/all-reviews");
        System.out.println(message);
        messagingTemplate.convertAndSend("/queue/all-reviews", message);//广播,前端筛选通知
    }
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "directQueueForActivityCreate",durable = "true") ,
            exchange = @Exchange(name = "directExchangeForActivity",type = ExchangeTypes.DIRECT),
            key ={"createAct"}
    ))
    public void ActivityMessage(Message message) {
        message.setStatus(0);
        List<User> admins = userClient.getAdmins();
        List<Long> ids = admins.stream().map(User::getId).toList(); //获取管理员的id集合
        String receiverStr = ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        for (User admin : admins) {
            message.setReceiver(String.valueOf(admin.getId()));
            messagingTemplate.convertAndSend("/queue/all-reviews", message);//广播,前端筛选通知
        }
        messageMapper.insertMessage(message.getContent(),message.getSender(),receiverStr,message.getSendTime(),message.getStatus());
    }
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "directQueueForActivityApprove",durable = "true") ,
            exchange = @Exchange(name = "directExchangeForActivity",type = ExchangeTypes.DIRECT),
            key ={"approveAct"}
    ))
    public void ActivityApproveMessage(Message message) {
        message.setStatus(0);
        messageMapper.insertMessage(message.getContent(),message.getSender(),message.getReceiver(),message.getSendTime(),message.getStatus());
        System.out.println(message);
            messagingTemplate.convertAndSend("/queue/all-reviews", message);//广播,前端筛选通知
    }
}
