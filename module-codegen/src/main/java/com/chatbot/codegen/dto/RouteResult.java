package com.chatbot.codegen.dto;

import com.chatbot.codegen.enums.CodegenStrategy;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RouteResult {
    private CodegenStrategy strategy;
    private String reason;
    private double confidence;
}
