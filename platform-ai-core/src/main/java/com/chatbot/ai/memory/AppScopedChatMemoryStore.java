package com.chatbot.ai.memory;

import com.chatbot.common.constant.CacheKeys;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 按 appId + sessionId 隔离的多轮对话记忆。
 */
@Component
@Primary
@RequiredArgsConstructor
public class AppScopedChatMemoryStore implements ChatMemoryStore {

    private static final long TTL_DAYS = 30;

    private final StringRedisTemplate redisTemplate;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = redisTemplate.opsForValue().get(toKey(memoryId));
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return ChatMessageDeserializer.messagesFromJson(json);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String key = toKey(memoryId);
        if (messages == null || messages.isEmpty()) {
            redisTemplate.delete(key);
            return;
        }
        redisTemplate.opsForValue().set(
                key,
                ChatMessageSerializer.messagesToJson(messages),
                TTL_DAYS,
                TimeUnit.DAYS
        );
    }

    @Override
    public void deleteMessages(Object memoryId) {
        redisTemplate.delete(toKey(memoryId));
    }

    private String toKey(Object memoryId) {
        return CacheKeys.CHAT_MEMORY_PREFIX + memoryId;
    }
}
