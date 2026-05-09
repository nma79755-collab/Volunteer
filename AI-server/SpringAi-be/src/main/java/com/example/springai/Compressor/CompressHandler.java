package com.example.springai.Compressor;

import com.example.springai.Repository.MongoRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
//压缩对话记忆
@Component
public class CompressHandler {
    @Autowired
    private ConversationCompressor conversationCompressor;
    @Autowired
    private MongoRepository mongoRepository;
    public String compressHistory(
            String conversationId) {
        List<Message> history =mongoRepository.get(conversationId);
        if (!conversationCompressor.isCompressed(history)) {
            return "当前对话历史不需要压缩。";
        }
        List<Message> compressed =conversationCompressor.compress(history);
        saveCompressedHistory(conversationId, compressed);
        return "压缩成功。";
    }
    private void saveCompressedHistory(String conversationId, List<Message> compressed) {
        mongoRepository.clear(conversationId);
        mongoRepository.add(conversationId, compressed);
    }
}
