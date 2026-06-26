package com.chatbot.codegen.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeRunResult {

    private boolean success;
    private String language;
    private String stdout;
    private String stderr;
    private int exitCode;
    private long durationMs;
    private String message;
}
