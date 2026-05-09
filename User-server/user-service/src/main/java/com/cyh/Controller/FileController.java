package com.cyh.Controller;

import com.cyh.Utils.FileUploadUtil;
import com.cyh.exception.BusinessException;
import com.cyh.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class FileController {
    @Autowired
    private FileUploadUtil fileUploadUtil;
    @Autowired
    private UserMapper userMapper;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/bmp",
            "image/svg+xml"
    );
    @PostMapping("/upload")
    public String uploadFile(@RequestParam("files") MultipartFile file, @RequestParam(required = false) Long userId) throws IOException {
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
            userMapper.insertAvatar(url, userId);
        return url;
    }
}
