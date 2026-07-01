package com.chatbot.app.dto;

import lombok.Data;

@Data
public class VisualEditRequest {
    private String filePath;
    private String elementSelector;
    private String elementText;
    private String instruction;
}
