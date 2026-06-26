package com.chatbot.appointment.controller;

import com.chatbot.appointment.ai.ChatAssistant;
import com.chatbot.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/api/appointment")
@RequiredArgsConstructor
public class AppointmentChatController {

    private final ChatAssistant chatAssistant;

    @PostMapping("/chat")
    public ApiResponse<String> chat(@RequestBody Map<String, String> body) {
        String sessionId = body.getOrDefault("sessionId", "default");
        String message = body.get("message");
        return ApiResponse.ok(chatAssistant.chat(sessionId, message));
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(
            @RequestParam String sessionId,
            @RequestParam String message) {
        return chatAssistant.chatStream(sessionId, message)
                .map(chunk -> ServerSentEvent.<String>builder().data(chunk).build());
    }
}
