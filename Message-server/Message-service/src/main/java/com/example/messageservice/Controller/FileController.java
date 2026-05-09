package com.example.messageservice.Controller;

import com.cyh.Client.UserClient;
import com.cyh.Utils.FileUploadUtil;
import com.cyh.Utils.UserContext;
import com.cyh.entity.ChatMessage;
import com.cyh.entity.Message;
import com.cyh.entity.User;
import com.cyh.exception.BusinessException;
import com.example.messageservice.mapper.ChatMessageMapper;
import com.example.messageservice.mapper.MessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
public class FileController {
    @Autowired
    private FileUploadUtil fileUploadUtil;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ChatMessageMapper chatMessageMapper;
    @Autowired
    private UserClient userClient;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/bmp",
            "image/svg+xml"
    );
    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,@RequestParam Long senderId,@RequestParam Long receiverId) throws IOException {
        if (file == null) {
            throw new BusinessException("请选择要上传的图片");
        }
            String contentType = file.getContentType();
            String originalFilename = file.getOriginalFilename();
            if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
                throw new BusinessException("不支持的图片格式，仅支持 jpg/png/gif/webp/bmp/svg");
            }
            long size = file.getSize();
            if (size > 1024 * 1024 * 10) {
                throw new BusinessException("图片大小不能超过10MB");
            }
            String cleanFilename = originalFilename.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
            String fileName = cleanFilename + UUID.randomUUID();
            String objectName = "user/" + fileName;
            String url = fileUploadUtil.upload(file.getBytes(), objectName);
            chatMessageMapper.insertChatMessage(url,senderId,receiverId, LocalDateTime.now(),0,2);
        ChatMessage message = new ChatMessage();
        message.setSenderId(UserContext.getUser());
        User receiver = userClient.getUserName(receiverId);
        message.setReceiverId(receiverId);
        message.setReceiverName(receiver.getUsername());
        message.setReceiverAvatar(receiver.getAvatar());
        User sender = userClient.getUserName(UserContext.getUser());
        message.setSenderName(sender.getUsername());
        message.setSenderAvatar(sender.getAvatar());
        message.setType(2);
        message.setStatus(0);
        message.setSendTime(LocalDateTime.now());
        message.setContent(url);
        System.out.println(senderId+"发送消息to"+receiverId+":"+message);
        messagingTemplate.convertAndSendToUser(String.valueOf(receiverId),"/queue/chat",message);
        return url;
    }
}
