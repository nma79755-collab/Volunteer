package com.example.springai.Classify;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Set;

//调用大模型自动对文档进行分类
@Component
public class DocumentClassify {
    @Autowired
    @Qualifier("classifyChatClient")//注入专门的模型
    @Lazy // 懒加载,都需要在模型初始化以后才能使用,但是这个工具又被第一个模型所依赖,所以需要打破循环依赖
    private ChatClient client;
    // 所有分类代码
    public static final String RECIPE = "recipe";           // 美食菜谱
    public static final String GAME = "game";               // 游戏攻略
    public static final String ACADEMIC = "academic";       // 学术论文
    public static final String TECH = "tech";               // 技术文档
    public static final String FINANCE = "finance";         // 金融财经
    public static final String LAW = "law";                 // 法律文书
    public static final String MEDICAL = "medical";         // 医疗健康
    public static final String LITERATURE = "literature";   // 文学小说
    public static final String NEWS = "news";               // 新闻资讯
    public static final String GENERAL = "general";         // 通用/其他
    public  String classify(String text){
        String sample = text.length() > 500 ? text.substring(0, 500) : text;//只取文档前500字
        String prompt="""
            请判断以下文档内容属于哪一类，只返回分类单词，不要解释。
            
            分类选项：
            - recipe：美食菜谱、饮品制作、烹饪教程
            - game：游戏攻略、游戏指南、游戏技巧
            - academic：学术论文、课程作业、研究报告、文献
            - tech：技术文档、编程教程、软件开发
            - finance：金融财经、投资理财、股票基金
            - law：法律文书、合同协议、法律法规
            - medical：医疗健康、疾病诊断、药品说明
            - literature：文学小说、散文诗歌、文学作品
            - news：新闻资讯、时事报道
            - general：通用其他（不属于以上任何类别）
            
            文档内容：
            %s
            """.formatted(sample);
        String res =client.prompt().user(prompt).call().content().trim().toLowerCase();//获取回答并转小写
        // 校验返回值是否在有效分类中
        Set<String> validCategories = Set.of(
                RECIPE, GAME, ACADEMIC, TECH, FINANCE,
                LAW, MEDICAL, LITERATURE, NEWS, GENERAL
        );
        return validCategories.contains(res) ? res : GENERAL;//如果不在就返回是通用
    }
}
