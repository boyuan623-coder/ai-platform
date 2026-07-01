package com.chatbot.codegen.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class VueProjectResult {
    private String sessionId;
    private String projectType;
    private String entryPath;
    private String summary;
    private String assistantReply;
    private Map<String, String> files;
}
