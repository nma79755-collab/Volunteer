package com.cyh.Config;

import com.cyh.Utils.FileUploadUtil;
import com.cyh.properties.OssProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OssConfig {
    @Autowired
    private OssProperties ossProperties;
    @Bean
    public FileUploadUtil fileUploadUtil(){
        FileUploadUtil fileUploadUtil = new FileUploadUtil();
        fileUploadUtil.setEndpoint(ossProperties.getEndpoint());
        fileUploadUtil.setAccessKeyId(ossProperties.getAccessKeyId());
        fileUploadUtil.setAccessKeySecret(ossProperties.getAccessKeySecret());
        fileUploadUtil.setBucketName(ossProperties.getBucketName());
        return fileUploadUtil;
    }
}
