package com.chatbot.chat.service;

import com.chatbot.chat.ai.AppChatAssistant;
import com.chatbot.chat.dto.ChatMessageRequest;
import com.chatbot.chat.dto.ChatMessageVO;
import com.chatbot.common.constant.CacheKeys;
import com.chatbot.app.entity.App;
import com.chatbot.app.service.AppService;
import com.chatbot.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class AppChatService {

    private final AppChatAssistant appChatAssistant;
    private final ChatHistoryService chatHistoryService;
    private final UserService userService;
    private final AppService appService;

    public ChatMessageVO chat(String token, Long appId, String sessionId, String message) {
        userService.requireUser(token);
        App app = appService.requireOwnedApp(token, appId);
        String memoryId = memoryId(appId, sessionId);

        saveUserMessage(token, appId, message);

        String context = buildContext(app, message);
        String reply = appChatAssistant.chat(memoryId, context);

        return saveAssistantMessage(token, appId, reply);
    }

    public Flux<String> chatStream(String token, Long appId, String sessionId, String message) {
        userService.requireUser(token);
        App app = appService.requireOwnedApp(token, appId);
        String memoryId = memoryId(appId, sessionId);

        saveUserMessage(token, appId, message);
        String context = buildContext(app, message);

        return appChatAssistant.chatStream(memoryId, context)
                .doOnComplete(() -> {
                    // streaming complete handled in controller with buffer
                });
    }

    public ChatMessageVO saveAssistantMessage(String token, Long appId, String content) {
        ChatMessageRequest req = new ChatMessageRequest();
        req.setAppId(appId);
        req.setRole("ASSISTANT");
        req.setContent(content);
        return chatHistoryService.save(token, req);
    }

    public void saveUserMessage(String token, Long appId, String message) {
        ChatMessageRequest req = new ChatMessageRequest();
        req.setAppId(appId);
        req.setRole("USER");
        req.setContent(message);
        chatHistoryService.save(token, req);
    }

    private String buildContext(App app, String message) {
        return """
                【应用名称】%s
                【应用描述】%s
                【代码类型】%s
                【用户消息】%s
                """.formatted(
                app.getName(),
                app.getDescription() != null ? app.getDescription() : "",
                app.getCodeType(),
                message
        );
    }

    private String memoryId(Long appId, String sessionId) {
        return CacheKeys.chatMemory(String.valueOf(appId), sessionId);
    }
}
