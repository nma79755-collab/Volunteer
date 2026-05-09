package com.cyh.Utils;

import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import lombok.Data;
import com.qiniu.storage.Configuration;
@Data
public class FileUploadUtil {
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    public String upload(byte[] bytes, String objectName) {


        // 1. 初始化七牛云配置（对应区域）
        Configuration cfg = new Configuration(Region.region2()); // 华南区域（z2）
        UploadManager uploadManager = new UploadManager(cfg);

        // 2. 生成上传凭证
        Auth auth = Auth.create(accessKeyId, accessKeySecret);
        String upToken = auth.uploadToken(bucketName);

        try {
            // 3. 执行上传（字节数组方式）
            uploadManager.put(bytes, objectName, upToken);
            System.out.println("http://tenusfane.hn-bkt.clouddn.com/" + objectName);
            // 4. 返回文件访问URL（需拼接自己的七牛云域名）
            return "http://tenusfane.hn-bkt.clouddn.com/" + objectName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
