package com.chatbot.app.dto;

import lombok.Data;

@Data
public class AppCreateRequest {
    private String name;
    private String description;
    private String codeType;
    private String codeContent;
}
