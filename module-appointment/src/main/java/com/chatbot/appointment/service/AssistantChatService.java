package com.chatbot.appointment.service;

import com.chatbot.appointment.ai.PlatformAssistant;
import com.chatbot.user.entity.User;
import com.chatbot.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class AssistantChatService {

    private final PlatformAssistant platformAssistant;
    private final AssistantHistoryService historyService;
    private final UserService userService;

    public String chat(String token, String sessionId, String message) {
        User user = userService.requireUser(token);
        historyService.ensureSession(user.getId(), sessionId, message);
        historyService.saveMessage(sessionId, user.getId(), "USER", message);
        String reply = platformAssistant.chat(sessionId, message);
        historyService.saveMessage(sessionId, user.getId(), "ASSISTANT", reply);
        return reply;
    }

    public Flux<String> chatStream(String token, String sessionId, String message) {
        User user = userService.requireUser(token);
        historyService.ensureSession(user.getId(), sessionId, message);
        historyService.saveMessage(sessionId, user.getId(), "USER", message);

        StringBuilder buffer = new StringBuilder();
        return platformAssistant.chatStream(sessionId, message)
                .doOnNext(buffer::append)
                .doOnComplete(() -> {
                    if (!buffer.isEmpty()) {
                        historyService.saveMessage(sessionId, user.getId(), "ASSISTANT", buffer.toString());
                    }
                });
    }
}
