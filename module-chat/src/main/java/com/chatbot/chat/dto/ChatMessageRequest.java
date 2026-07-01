package com.chatbot.chat.dto;

import lombok.Data;

@Data
public class ChatMessageRequest {
    private Long appId;
    private String role;
    private String content;
}
