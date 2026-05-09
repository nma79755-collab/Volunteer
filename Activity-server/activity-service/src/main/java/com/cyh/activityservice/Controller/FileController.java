package com.cyh.activityservice.Controller;

import com.cyh.Utils.FileUploadUtil;
import com.cyh.activityservice.mapper.ActivityMapper;
import com.cyh.activityservice.properties.OssProperties;
import com.cyh.exception.BusinessException;
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
@RequestMapping("/api/activities")
public class FileController {
    @Autowired
    private FileUploadUtil fileUploadUtil;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/bmp",
            "image/svg+xml"
    );
    @PostMapping("/upload")
    public List<String> uploadFile(@RequestParam("files") MultipartFile[] files, @RequestParam(required = false) String activityId) throws IOException {
        List<String> urls = new ArrayList<>();
        if (files == null || files.length == 0) {
            throw new BusinessException("请选择要上传的图片");
        }
        int i=0;
        for (MultipartFile file : files) {
            i++;
            if (file == null || file.isEmpty()) {
                continue;
            }
            String contentType = file.getContentType();
            String originalFilename = file.getOriginalFilename();
            if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
                throw new BusinessException("第"+i+"张图片是"+"不支持的图片格式，仅支持 jpg/png/gif/webp/bmp/svg");
            }
            long size = file.getSize();
            if (size > 1024 * 1024 * 10) {
                throw new BusinessException("第"+i+"张"+"图片大小不能超过10MB");
            }
            String cleanFilename = originalFilename.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
            String fileName = cleanFilename + UUID.randomUUID();
            String objectName = "activity/" + fileName;
            String url = fileUploadUtil.upload(file.getBytes(), objectName);
           urls.add(url);
        }
        if (urls.isEmpty()) {
            throw new BusinessException("没有有效的图片文件");
        }
        return urls;
    }
}
