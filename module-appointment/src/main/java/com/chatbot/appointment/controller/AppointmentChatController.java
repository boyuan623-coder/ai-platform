package com.chatbot.appointment.controller;

import com.chatbot.appointment.service.AssistantChatService;
import com.chatbot.ai.support.ReactiveBlocking;
import com.chatbot.common.api.ApiResponse;
import com.chatbot.user.auth.TokenAuthSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/appointment")
@RequiredArgsConstructor
public class AppointmentChatController {

    private final AssistantChatService assistantChatService;
    private final TokenAuthSupport tokenAuthSupport;

    @PostMapping("/chat")
    public Mono<ApiResponse<String>> chat(
            @RequestHeader("X-Auth-Token") String token,
            @RequestBody Map<String, String> body) {
        tokenAuthSupport.requireAuth(token);
        String sessionId = body.getOrDefault("sessionId", "default");
        String message = body.get("message");
        return ReactiveBlocking.mono(() -> assistantChatService.chat(token, sessionId, message))
                .map(ApiResponse::ok);
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam String sessionId,
            @RequestParam String message) {
        tokenAuthSupport.requireAuth(token);
        return assistantChatService.chatStream(token, sessionId, message)
                .map(chunk -> ServerSentEvent.<String>builder().data(chunk).build());
    }
}
