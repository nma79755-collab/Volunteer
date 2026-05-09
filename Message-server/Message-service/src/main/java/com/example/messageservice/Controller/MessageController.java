package com.example.messageservice.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.cyh.Client.UserClient;
import com.cyh.Res.ApiResponse;
import com.cyh.Utils.UserContext;
import com.cyh.entity.ChatMessage;
import com.cyh.entity.Message;
import com.cyh.entity.User;
import com.example.messageservice.mapper.ChatMessageMapper;
import com.example.messageservice.mapper.MessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/messages")
public class MessageController {
    
    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ChatMessageMapper chatMessageMapper;
    @Autowired
    private UserClient userClient;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getMyMessages() {
        log.info("Get my messages endpoint called");

        Long userId = UserContext.getUser();
        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "User not authenticated"));
        }
        
        QueryWrapper<Message> qw = new QueryWrapper<>();
        qw.apply("FIND_IN_SET({0}, receiver)", userId) //获取String中以,分割的匹配
                .orderByDesc("sendTime");
        
        List<Message> messages = messageMapper.selectList(qw);
        return ResponseEntity.ok(ApiResponse.success("Messages retrieved successfully", messages));
    }
    @PutMapping("/status")
    public void MessageStatus(){
        Long userId = UserContext.getUser();
        messageMapper.update(new UpdateWrapper<Message>().apply("FIND_IN_SET({0}, receiver)", userId).set("status",1));
    }
    @GetMapping("/UnRead")
    public ResponseEntity<ApiResponse<?>> MessageUnRead(){
        Long userId = UserContext.getUser();
        Long messages = messageMapper.selectCount(new QueryWrapper<Message>().apply("FIND_IN_SET({0}, receiver)", userId).eq("status", 0));
        return ResponseEntity.ok(ApiResponse.success("获取成功", messages));
    }
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<?>> chat(@RequestBody ChatMessage message) {
    message.setSenderId(UserContext.getUser());
        Long receiverId = message.getReceiverId();
        User receiver = userClient.getUserName(receiverId);
        message.setReceiverName(receiver.getUsername());
        message.setReceiverAvatar(receiver.getAvatar());
        User sender = userClient.getUserName(UserContext.getUser());
        message.setSenderName(sender.getUsername());
        message.setSenderAvatar(sender.getAvatar());
        message.setStatus(0);
        message.setSendTime(LocalDateTime.now());
        chatMessageMapper.insert(message);
        System.out.println(message.getSenderId()+"发送消息to"+receiverId+":"+message);
        messagingTemplate.convertAndSendToUser(String.valueOf(receiverId),"/queue/chat",message);
        return ResponseEntity.ok(ApiResponse.success("发送成功"));
    }
    @GetMapping("/GetChat")
    public ResponseEntity<ApiResponse<?>> GetChat(){
        Long userId = UserContext.getUser();
        QueryWrapper<ChatMessage> qw = new QueryWrapper<>();
        qw.eq("senderId", userId).or().eq("receiverId", userId);
        return ResponseEntity.ok(ApiResponse.success("获取成功", chatMessageMapper.selectList(qw)));
    }
    @GetMapping("/GetChatUnRead")
    public ResponseEntity<ApiResponse<?>> GetChatUnRead(){
        Long userId = UserContext.getUser();
        QueryWrapper<ChatMessage> qw = new QueryWrapper<>();
        qw.eq("receiverId", userId).eq("status", 0);
        return ResponseEntity.ok(ApiResponse.success("获取成功", chatMessageMapper.selectCount(qw)));
    }
    @PutMapping("/ReadChat")
    public ResponseEntity<ApiResponse<?>> ReadChat(@RequestParam Long senderId){
        if(chatMessageMapper.selectList(new QueryWrapper<ChatMessage>().eq("status", 0).eq("senderId", senderId).eq("receiverId", UserContext.getUser()))!=null) {
            UpdateWrapper<ChatMessage> wrapper = new UpdateWrapper<>();
            wrapper.set("status", 1)
                    .eq("status", 0)
                    .eq("receiverId", UserContext.getUser())
                    .eq("senderId", senderId);

            chatMessageMapper.update(wrapper);
            messagingTemplate.convertAndSendToUser(String.valueOf(senderId), "/queue/chat", UserContext.getUser()+"已读");//发送消息标记已读
        }
        return ResponseEntity.ok(ApiResponse.success("已读成功"));
    }
}
