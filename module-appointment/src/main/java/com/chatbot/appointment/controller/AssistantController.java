package com.chatbot.appointment.controller;

import com.chatbot.appointment.dto.AssistantMessageVO;
import com.chatbot.appointment.dto.AssistantSessionVO;
import com.chatbot.appointment.service.AssistantChatService;
import com.chatbot.appointment.service.AssistantHistoryService;
import com.chatbot.ai.support.ReactiveBlocking;
import com.chatbot.common.api.ApiResponse;
import com.chatbot.user.entity.User;
import com.chatbot.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantChatService chatService;
    private final AssistantHistoryService historyService;
    private final UserService userService;

    @GetMapping("/sessions")
    public ApiResponse<List<AssistantSessionVO>> listSessions(
            @RequestHeader("X-Auth-Token") String token) {
        User user = userService.requireUser(token);
        return ApiResponse.ok(historyService.listSessions(user.getId()));
    }

    @PostMapping("/sessions")
    public ApiResponse<AssistantSessionVO> createSession(
            @RequestHeader("X-Auth-Token") String token,
            @RequestBody(required = false) Map<String, String> body) {
        User user = userService.requireUser(token);
        String sessionId = body != null && body.get("sessionId") != null
                ? body.get("sessionId")
                : "s-" + UUID.randomUUID().toString().replace("-", "");
        String title = body != null ? body.getOrDefault("title", "新对话") : "新对话";
        return ApiResponse.ok(historyService.createSession(user.getId(), sessionId, title));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<AssistantMessageVO>> listMessages(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable String sessionId) {
        User user = userService.requireUser(token);
        return ApiResponse.ok(historyService.listMessages(user.getId(), sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Void> deleteSession(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable String sessionId) {
        User user = userService.requireUser(token);
        historyService.deleteSession(user.getId(), sessionId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/chat")
    public Mono<ApiResponse<String>> chat(
            @RequestHeader("X-Auth-Token") String token,
            @RequestBody Map<String, String> body) {
        String sessionId = body.getOrDefault("sessionId", "default");
        String message = body.get("message");
        return ReactiveBlocking.mono(() -> chatService.chat(token, sessionId, message))
                .map(ApiResponse::ok);
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam String sessionId,
            @RequestParam String message) {
        return chatService.chatStream(token, sessionId, message)
                .map(chunk -> ServerSentEvent.<String>builder().data(chunk).build());
    }
}
