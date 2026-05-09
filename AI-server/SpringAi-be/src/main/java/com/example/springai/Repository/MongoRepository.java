package com.example.springai.Repository;

import com.example.springai.Entity.ChatMessage;
import com.example.springai.Entity.MongoChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

//用mongodb来存储对话记忆
@Repository
@RequiredArgsConstructor
public class MongoRepository implements ChatMemory {
    private final MongoTemplate mongoTemplate;
    private static final int DEFAULT_MAX_MESSAGES = 20;//定义滑动窗口大小
    @Override
    public void add(String conversationId, List<Message> messages) {
        List<MongoChatMessage> collect = messages.stream().map(msg -> MongoChatMessage.from(conversationId, msg)).toList();
        mongoTemplate.insertAll(collect);//批量插入数据进会话记忆表
        //TODO 可以改成mq异步执行
        List<ChatMessage> list = messages.stream().map(msg -> ChatMessage.chatMessage(conversationId, msg)).toList();
        mongoTemplate.insertAll(list);//插入历史表
        cleanOldMessages(conversationId,DEFAULT_MAX_MESSAGES);//清理大于滑动窗口的数据
    }

    @Override
    public List<Message> get(String conversationId) { //用于查询mongo中的数据并转化为springai需要的类型
        Query query = new Query(Criteria.where("conversationId").is(conversationId))
                .with(Sort.by(Sort.Direction.ASC, "time"));//定义查询规则
        List<MongoChatMessage> mongoChatMessages = mongoTemplate.find(query, MongoChatMessage.class);//查询出mongo中的数据
        List<Message> collect = mongoChatMessages.stream().map(MongoChatMessage::toMessage).collect(Collectors.toList());
        return collect;
    }

    @Override
    public void clear(String conversationId) {
        Query query = new Query(Criteria.where("conversationId").is(conversationId));
        mongoTemplate.remove(query, MongoChatMessage.class);
    }

    private void cleanOldMessages(String conversationId, int maxMessages) {
        Query countQuery = new Query(Criteria.where("conversationId").is(conversationId));
        long count = mongoTemplate.count(countQuery, MongoChatMessage.class);
        if (count > maxMessages) {
            // 找到需要删除的旧消息
            Query oldMessagesQuery = new Query(Criteria.where("conversationId").is(conversationId))
                    .with(Sort.by(Sort.Direction.ASC, "time"))
                    .limit((int) (count - maxMessages));

            List<MongoChatMessage> toDelete = mongoTemplate.find(oldMessagesQuery, MongoChatMessage.class);
            for (MongoChatMessage msg : toDelete) {
                mongoTemplate.remove(msg);
            }
        }
    }
}
