package com.chatbot.appointment.config;

import com.chatbot.appointment.ai.ChatAssistant;
import com.chatbot.appointment.tool.AppointmentTools;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppointmentAiConfig {

    @Bean
    public ChatAssistant chatAssistant(
            ChatModel chatModel,
            StreamingChatModel streamingChatModel,
            ChatMemoryProvider chatMemoryProvider,
            ContentRetriever contentRetriever,
            AppointmentTools appointmentTools) {

        return AiServices.builder(ChatAssistant.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(memoryId ->
                        chatMemoryProvider.get("chat:" + memoryId))
                .contentRetriever(contentRetriever)
                .tools(appointmentTools)
                .build();
    }
}
