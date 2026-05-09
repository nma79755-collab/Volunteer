package com.example.springai.Config;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.FileSystemResource;

import java.util.Arrays;
import java.util.List;
//文件加载器和文件分割器
public class DocumentLoader {
    public static List<Document> load(String path){
        FileSystemResource resource = new FileSystemResource(path);//将文件解析为可用于加载器的格式
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(resource);//初始化文档加载器
        List<Document> docs = tikaDocumentReader.read();//加载文档
        List<Character> separators = Arrays.asList('\n', '。', '!', '?', ';');//定义分割标志
        TokenTextSplitter splitter = new TokenTextSplitter(
                500,//段落最大长度
                50,//运行重复字符数
                0,
                100,
                true,
                separators
        );
        List<Document> documents = splitter.split(docs);
        System.out.println(documents);
        return documents;
    }
}
