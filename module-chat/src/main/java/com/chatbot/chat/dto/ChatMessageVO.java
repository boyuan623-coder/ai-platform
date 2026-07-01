package com.chatbot.chat.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageVO {
    private Long id;
    private Long appId;
    private String role;
    private String content;
    private LocalDateTime createdAt;
}
