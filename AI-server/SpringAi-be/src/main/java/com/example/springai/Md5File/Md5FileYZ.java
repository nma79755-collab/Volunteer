package com.example.springai.Md5File;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
public class Md5FileYZ {
    private String path ="E:\\pineapple\\backend\\AI-server\\SpringAi-be\\files\\md5.txt";
    public Boolean md5FileYZ(MultipartFile file, String Path) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String md5 = DigestUtils.md5DigestAsHex(file.getInputStream());
        System.out.println("文件MD5为:" + md5);

        String fileMd5 = getFileMd5();
        if (fileMd5 != null && fileMd5.contains(md5)) {
            System.out.println("MD5已存在，抛出异常");
            throw new RuntimeException("文件已存在");
        }
        System.out.println("MD5检查通过");

        // 检查保存路径
        File targetDir = new File(Path);
        System.out.println("保存目录: " + targetDir.getAbsolutePath());
        System.out.println("目录是否存在: " + targetDir.exists());
        System.out.println("目录是否可写: " + targetDir.canWrite());

        File targetFile = new File(Path, originalFilename);
        System.out.println("目标文件: " + targetFile.getAbsolutePath());

        try {
            file.transferTo(targetFile);
            System.out.println("文件保存成功");
        } catch (Exception e) {
            System.out.println("文件保存失败: " + e.getMessage());
            throw new RuntimeException("文件保存失败: " + e.getMessage());
        }

        // 写入MD5
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path, true))) {
            writer.write(md5);
            writer.newLine();
            writer.flush();
            System.out.println("MD5写入成功");
        } catch (Exception e) {
            System.out.println("MD5写入失败: " + e.getMessage());
            throw new RuntimeException("MD5写入失败: " + e.getMessage());
        }

        return true;
    }
    public String getFileMd5() throws IOException {
        File file = new File(path);
        if(!file.exists()){
            file.createNewFile(); //如果md5目录文件不存在就创建
            return null;
        }
        try{
            StringBuilder stringBuilder = new StringBuilder();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                // 如果想跳过空行，加这个判断
                if (line.trim().isEmpty()) {
                    continue;  // 跳过空行
                }
                stringBuilder.append(line).append("\n");
            }
            bufferedReader.close();
            return stringBuilder.toString(); //获取所有的文件的md5
        }catch (Exception e){
            throw new RuntimeException("文件不存在");
        }
    }
}
