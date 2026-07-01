package com.chatbot.codegen.config;

import com.chatbot.codegen.ai.VueProjectAssistant;
import com.chatbot.codegen.tool.VueProjectTools;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CodeGenAiConfig {

    @Bean
    public VueProjectAssistant vueProjectAssistant(
            ChatModel chatModel,
            ChatMemoryProvider chatMemoryProvider,
            VueProjectTools vueProjectTools) {
        // Vue 工程生成依赖 Tool Calling，使用同步 ChatModel（与 StreamingChatModel 二选一）
        return AiServices.builder(VueProjectAssistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> chatMemoryProvider.get("codegen:vue:" + memoryId))
                .tools(vueProjectTools)
                .build();
    }
}
