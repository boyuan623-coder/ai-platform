package com.chatbot.chat.config;

import com.chatbot.chat.ai.AppChatAssistant;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppChatAiConfig {

    @Bean
    public AppChatAssistant appChatAssistant(
            ChatModel chatModel,
            StreamingChatModel streamingChatModel,
            ChatMemoryProvider chatMemoryProvider) {
        return AiServices.builder(AppChatAssistant.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(memoryId -> chatMemoryProvider.get(memoryId))
                .build();
    }
}
