package com.example.springai.Service.Impl;

import com.example.springai.Classify.DocumentClassify;
import com.example.springai.Config.DocumentLoader;
import com.example.springai.Service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {
    private final VectorStore vectorStore;
    @Autowired
    private DocumentClassify documentClassify;
    @Override
    public String askByRag(String question) {
        String classify = documentClassify.classify(question);
        System.out.println("问题分类为"+classify);
        List<Document> documentList = vectorStore.similaritySearch(SearchRequest.builder().query(question).
                topK(2)//返回最相似的两个
                        .similarityThreshold(0.5) //过滤distance大于0.5的文档
                        .filterExpression("category == '" + classify + "'") //只查询该分类下的文档
                .build());//返回检索到的文档

        // 如果同分类没结果，回退到全库检索
        if (documentList.isEmpty()&&!classify.equals("general")) {
            System.out.println("分类 [" + classify + "] 无结果，回退全库");
             documentList = vectorStore.similaritySearch(SearchRequest.builder().query(question).
                     topK(2)
                     .similarityThreshold(0.6)
                     .build());
        }
        StringBuilder doc=new StringBuilder();
        for (int i = 0; i < documentList.size(); i++) {
            doc.append("【片段").append(i + 1).append("】\n");
            doc.append(documentList.get(i).getText());
            doc.append("\n---\n");
            System.out.println("片段" + (i+1) + " 来源: " + documentList.get(i).getMetadata().get("source"));
            System.out.println("片段" + (i+1) + " 距离: " + documentList.get(i).getMetadata().get("distance"));
            System.out.println("片段" + (i+1) + " 分类: " + documentList.get(i).getMetadata().get("category"));
        }
        return doc.toString();
    }
}
