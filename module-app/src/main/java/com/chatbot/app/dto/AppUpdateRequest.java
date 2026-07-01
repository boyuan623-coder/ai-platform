package com.chatbot.app.dto;

import lombok.Data;

@Data
public class AppUpdateRequest {
    private String name;
    private String description;
    private String codeType;
    private String codeContent;
}
