package com.example.springai.KeySearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


@Component
public class RagByKey {
    private final SmartChineseAnalyzer analyzer= new SmartChineseAnalyzer(); //初始化一个切词器,用于将用户问题切割为关键词
    private Directory directory; //指定索引存储库
    private  IndexWriter indexWriter;//写入索引类
    private String collectionId = null; // 缓存 collection ID

    @PostConstruct
    public void initIndex() throws IOException {
        List<org.springframework.ai.document.Document> documents = getAllDocuments();//获取所有文档
        directory = new ByteBuffersDirectory(); //指定一个存储在内存的临时索引存储库
        IndexWriterConfig config = new IndexWriterConfig(analyzer);//指定索引写入配置
         indexWriter = new IndexWriter(directory, config);//指定索引配置和写入位置
        for (org.springframework.ai.document.Document doc:documents
             ) {
            System.out.println("片段的" + " 分类为: " + doc.getMetadata().get("category"));
            Document document = new Document(); //创建一个 Lucene 文档对象
            document.add(new StringField("id",doc.getId(), Field.Store.YES));//将文档存入,StringField表示该字段不会被切词,Field.Store.YES表示保留原始值,将chroma中的文档id存入document
            document.add(new TextField("content",doc.getText(),Field.Store.YES));//TextField表示内容会被切词用来检索
            indexWriter.addDocument(document);//将处理好的每条文档写入索引它会：用分词器把 content 字段切词,建立倒排索引(每个词 → 出现在哪些文档里),把 id 原样存储
        }
        indexWriter.commit();  // 把内存里的索引提交到dictionary
        indexWriter.close();   // 关闭写入器，释放资源
    }
    public String getDocByKey(String question, int topK) throws IOException, ParseException {
        if (directory == null) {
            return Collections.emptyList().toString(); // 索引库为空,直接返回空集合
        }
        DirectoryReader reader = DirectoryReader.open(directory);//打开索引库以供查找
        IndexSearcher searcher = new IndexSearcher(reader);//指定搜索引擎，传入索引库
        searcher.setSimilarity(new BM25Similarity());//设定搜索引擎的打分机制为BM25
        QueryParser parser = new QueryParser("content", analyzer);//在文档的content字段里查找,用切词器切割提问
        Query query = parser.parse(question);//用上面指定的切词器切割提问
        TopDocs search = searcher.search(query, topK);//拿到符合的文档，指定数量TopDocs 是搜索结果容器，包含两个关键信息：totalHits：总共匹配到多少条文档,scoreDocs：排好序的结果列表，每个元素包含文档内部编号和BM25 得分
        StringBuilder stringBuilder = new StringBuilder();
        System.out.println("BM检索到文档数"+search.totalHits);
        for (ScoreDoc scoreDoc : search.scoreDocs) {
            Document document = searcher.storedFields().document(scoreDoc.doc);//scoreDoc.doc是Lucene 文档的编号,自动分配
             String content = document.get("content");//获取文档内容
            org.springframework.ai.document.Document doc = new org.springframework.ai.document.Document(content);//将内容写回springAi的document
            stringBuilder.append(doc.getText());
        }
        reader.close();//释放资源
        return stringBuilder.toString();
    }
    //获取所有文档
    public List<org.springframework.ai.document.Document> getAllDocuments() {
        String collectionId = getCollectionId("chroma_db");

        String url = "http://127.0.0.1:8000/api/v2/tenants/default_tenant/databases/default_database/collections/"
                + collectionId + "/get";  // 使用 UUID，不是名称
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> body = Map.of(
                "limit", 10000,
                "offset", 0,
                "include", List.of("metadatas", "documents")
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(url, body, Map.class);
        Map<String, Object> result = response.getBody();

        List<String> ids = (List<String>) result.get("ids");
        List<String> texts = (List<String>) result.get("documents");
        List<Map<String, Object>> metadatas = (List<Map<String, Object>>) result.get("metadatas");

        List<org.springframework.ai.document.Document> docs = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            org.springframework.ai.document.Document doc = new org.springframework.ai.document.Document(texts.get(i), metadatas.get(i));
            docs.add(doc);
        }
        System.out.println("chroma中文档数"+docs.size());
        return docs;
    }


    /**
     * 获取 collection 的 UUID
     */
    private String getCollectionId(String collectionName) {
        if (collectionId != null) {
            return collectionId;
        }

        String url = "http://127.0.0.1:8000/api/v2/tenants/default_tenant/databases/default_database/collections";

        RestClient restClient = RestClient.create();

        String response = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode collections = mapper.readTree(response);

            for (JsonNode collection : collections) {
                if (collectionName.equals(collection.get("name").asText())) {
                    collectionId = collection.get("id").asText();
                    return collectionId;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        throw new RuntimeException("Collection not found: " + collectionName);
    }
}
