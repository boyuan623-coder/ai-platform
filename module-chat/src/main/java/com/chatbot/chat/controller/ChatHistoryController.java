package com.chatbot.chat.controller;

import com.chatbot.chat.dto.ChatHistoryPage;
import com.chatbot.chat.dto.ChatMessageRequest;
import com.chatbot.chat.dto.ChatMessageVO;
import com.chatbot.chat.service.ChatHistoryService;
import com.chatbot.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final ChatHistoryService chatHistoryService;

    @PostMapping("/message")
    public ApiResponse<ChatMessageVO> save(
            @RequestHeader("X-Auth-Token") String token,
            @RequestBody ChatMessageRequest request) {
        return ApiResponse.ok(chatHistoryService.save(token, request));
    }

    @GetMapping("/history")
    public ApiResponse<ChatHistoryPage> list(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam Long appId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(chatHistoryService.listByApp(token, appId, cursor, size));
    }

    @DeleteMapping("/history")
    public ApiResponse<Void> delete(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam Long appId) {
        chatHistoryService.deleteByApp(token, appId);
        return ApiResponse.ok(null);
    }
}
