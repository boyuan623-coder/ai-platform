package com.chatbot.app.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class AppProjectPayload {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String projectType;
    private String entryPath;
    private Map<String, String> files = new HashMap<>();

    public static AppProjectPayload fromCodeContent(String codeContent, String codeType) {
        if (codeContent == null || codeContent.isBlank()) {
            return null;
        }
        String trimmed = codeContent.trim();
        if (trimmed.startsWith("{")) {
            try {
                return MAPPER.readValue(trimmed, AppProjectPayload.class);
            } catch (Exception ignored) {
                // fall through to single file
            }
        }
        AppProjectPayload payload = new AppProjectPayload();
        payload.setProjectType(codeType != null ? codeType : "HTML");
        if ("VUE".equalsIgnoreCase(codeType) || "vue".equalsIgnoreCase(codeType)) {
            payload.setEntryPath("src/App.vue");
            payload.getFiles().put("src/App.vue", codeContent);
        } else if ("HTML".equalsIgnoreCase(codeType)) {
            payload.setEntryPath("index.html");
            payload.getFiles().put("index.html", codeContent);
        } else {
            payload.setEntryPath("main.txt");
            payload.getFiles().put("main.txt", codeContent);
        }
        return payload;
    }

    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (Exception e) {
            throw new IllegalStateException("序列化项目失败", e);
        }
    }

    public static Map<String, String> parseFilesMap(String json) {
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
