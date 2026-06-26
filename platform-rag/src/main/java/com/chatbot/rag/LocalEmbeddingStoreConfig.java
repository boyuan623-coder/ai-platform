package com.chatbot.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 本地 Redis 3.x 不支持 RediSearch 向量索引，local 环境使用内存向量库。
 * 会话记忆、结果缓存仍走本地 Redis（localhost:6379）。
 */
@Configuration
@Profile("local")
public class LocalEmbeddingStoreConfig {

    @Bean
    @Primary
    public EmbeddingStore<TextSegment> localEmbeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }
}
