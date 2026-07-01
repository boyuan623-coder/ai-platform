package com.chatbot.appointment.config;

import com.chatbot.appointment.ai.PlatformAssistant;
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
    public PlatformAssistant platformAssistant(
            ChatModel chatModel,
            StreamingChatModel streamingChatModel,
            ChatMemoryProvider chatMemoryProvider,
            ContentRetriever contentRetriever,
            AppointmentTools appointmentTools) {

        return AiServices.builder(PlatformAssistant.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(memoryId ->
                        chatMemoryProvider.get("appointment:" + memoryId))
                .contentRetriever(contentRetriever)
                .tools(appointmentTools)
                .build();
    }
}
