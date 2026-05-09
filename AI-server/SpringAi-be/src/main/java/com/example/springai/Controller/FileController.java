package com.example.springai.Controller;

import com.cyh.exception.FileLoadException;
import com.example.springai.Classify.DocumentClassify;
import com.example.springai.Config.DocumentLoader;
import com.example.springai.Md5File.Md5FileYZ;
import com.example.springai.Utils.OrcUtil;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/api/ai")
public class FileController {
    private final String path="E:\\pineapple\\backend\\AI-server\\SpringAi-be\\files";
    @Autowired
    private VectorStore vectorStore;
    @Autowired
    private DocumentClassify documentClassify;
    @Autowired
    private Md5FileYZ md5FileYZ;
    @Autowired
    private OrcUtil orcUtil;
    private static final int MaxSize = 10*1024*1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "text/plain", "text/markdown", "text/x-markdown",
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
            "application/vnd.openxmlformats-officedocument.presentationml.presentation" // .pptx
    );
    @PostMapping("/load")
    public String load(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        File fileEx = new File(path,originalFilename);
        if(fileEx.exists()){
            throw  new FileLoadException("文档已经上传过");
        }
        String contentType = file.getContentType();
        System.out.println("上传的文档类型为"+contentType);
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("不支持的文件类型：" + contentType);
        }
        if (file.getSize() > MaxSize) {
            throw new IllegalArgumentException("文件过大，请上传小于10MB的文件");
        }
        File f = new File(path,originalFilename);
        if (!f.getParentFile().exists()) {
            f.getParentFile().mkdirs(); // 自动创建父目录
        }
        if (!md5FileYZ.md5FileYZ(file,path)){
            throw new FileLoadException("文件已存在");//根据md5判断文件是否已存在
        }
        List<Document> documents = DocumentLoader.load(path + "\\" + originalFilename);

        String fullText = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));//将文档内容提取

        //TODO 异步分类文档
        String classify = documentClassify.classify(fullText);
        System.out.println("上传的文档分类为"+classify);
        for (Document doc : documents) {
            doc.getMetadata().put("category", classify); //将文档类型标记给文档
            System.out.println("文档片段 metadata: " + doc.getMetadata());
        }
        try {
            vectorStore.add(documents);
            System.out.println("向量库存储成功");
        }
        catch (Exception e)
        {
            throw new RuntimeException("向量库存储失败");
        }
        return path + "\\" + originalFilename;
    }
    @GetMapping("/download")
    public void download(String url, HttpServletResponse response) throws IOException {
        File file = new File(url);
        if(!file.exists()){
            throw new RuntimeException("文件不存在!");
        }
        if(!file.getCanonicalPath().startsWith(path)) //确保只能下载文件夹里的文件
        {
            throw new RuntimeException("不能访问的下载地址!");
        }
        String name = file.getName();
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode(name, "UTF-8") + "\"");
        ServletOutputStream outputStream = response.getOutputStream();
        FileInputStream fileInputStream = new FileInputStream(file);
        FileCopyUtils.copy(fileInputStream,outputStream);
        outputStream.flush();//刷新流确保完全写出
        fileInputStream.close();
    }
    @PostMapping("/loadImage")
    public String loadImage(MultipartFile file) throws IOException, TesseractException {
        String res = orcUtil.orc(file);
        return res;
    }
//    @DeleteMapping("/deleteDoc")
//    public String deleteDocument(@RequestParam("path") String path) {
//        File file = new File(path);
//        String name = file.getName();
//        if (file.exists()){
//            file.delete();
//        }
//        //删除向量库中的记录
//        vectorStore.delete(Filter.Expression("source == '" + name + "'"));
//        return "文档 [" + name + "] 已从向量库和本地删除";
//    }
}
