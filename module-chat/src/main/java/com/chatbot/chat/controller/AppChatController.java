package com.chatbot.chat.controller;

import com.chatbot.chat.dto.ChatMessageVO;
import com.chatbot.chat.service.AppChatService;
import com.chatbot.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class AppChatController {

    private final AppChatService appChatService;

    @PostMapping("/{appId}/message")
    public ApiResponse<ChatMessageVO> chat(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long appId,
            @RequestBody Map<String, String> body) {
        String sessionId = body.getOrDefault("sessionId", "default");
        String message = body.get("message");
        return ApiResponse.ok(appChatService.chat(token, appId, sessionId, message));
    }

    @GetMapping(value = "/{appId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long appId,
            @RequestParam String sessionId,
            @RequestParam String message) {
        AtomicReference<StringBuilder> buffer = new AtomicReference<>(new StringBuilder());

        return appChatService.chatStream(token, appId, sessionId, message)
                .map(chunk -> {
                    buffer.get().append(chunk);
                    return ServerSentEvent.<String>builder().data(chunk).build();
                })
                .doOnComplete(() -> {
                    String full = buffer.get().toString();
                    if (!full.isBlank()) {
                        appChatService.saveAssistantMessage(token, appId, full);
                    }
                });
    }
}
