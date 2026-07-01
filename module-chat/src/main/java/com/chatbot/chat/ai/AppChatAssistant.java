package com.chatbot.chat.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

public interface AppChatAssistant {

    @SystemMessage("""
            你是 AI 零代码平台的应用开发助手，帮助用户迭代和改进他们的 Web 应用。
            
            你可以：
            · 理解用户需求，给出实现建议
            · 说明如何修改页面结构、样式与交互
            · 在用户描述变更时，输出清晰的分步修改方案
            
            回答要求：分段清晰、列表每项单独一行、语气专业友好。
            若用户要求直接改代码，说明将修改哪些文件及要点（代码生成页可保存到应用）。
            """)
    String chat(@MemoryId String memoryId, @UserMessage String message);

    @SystemMessage("""
            你是 AI 零代码平台的应用开发助手。回答分段清晰，列表每项单独一行。
            """)
    Flux<String> chatStream(@MemoryId String memoryId, @UserMessage String message);
}
