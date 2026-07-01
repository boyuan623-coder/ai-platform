package com.chatbot.codegen.service;

import com.chatbot.codegen.dto.RouteResult;
import com.chatbot.codegen.enums.CodegenStrategy;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRouteService {

    private final ChatModel chatModel;

    @Value("${langchain4j.open-ai.chat-model.api-key:}")
    private String apiKey;

    public RouteResult route(String requirement) {
        validateApiKey();

        CodegenStrategy ruleStrategy = ruleBasedRoute(requirement);
        if (isConfidentRule(requirement, ruleStrategy)) {
            return RouteResult.builder()
                    .strategy(ruleStrategy)
                    .reason("规则路由: " + ruleStrategy.name())
                    .confidence(0.88)
                    .build();
        }

        try {
            String prompt = """
                    你是 AI 代码生成路由专家。根据用户需求选择唯一生成策略，只返回策略代码，不要解释：
                    VUE_PROJECT - 完整 Vue 3 应用、多组件、工程化项目
                    HTML_PAGE - 单页面 HTML 应用（计算器、落地页等）
                    SINGLE_CODE - 单文件代码片段（Java/Python/JS 算法、Hello World）
                    
                    用户需求：%s
                    """.formatted(requirement);

            String response = chatModel.chat(prompt).trim().toUpperCase();
            CodegenStrategy aiStrategy = parseStrategy(response);
            if (aiStrategy != null) {
                return RouteResult.builder()
                        .strategy(aiStrategy)
                        .reason("AI 智能路由: " + response)
                        .confidence(0.9)
                        .build();
            }
        } catch (Exception e) {
            log.warn("AI route failed, fallback to rules: {}", e.getMessage());
        }

        return RouteResult.builder()
                .strategy(ruleStrategy)
                .reason("规则路由（兜底）")
                .confidence(0.7)
                .build();
    }

    private void validateApiKey() {
        if (apiKey == null || apiKey.isBlank() || apiKey.contains("your-deepseek-api-key")) {
            throw new IllegalStateException(
                    "DeepSeek API Key 未配置或无效，请在 application-secrets.yml 或环境变量 DEEPSEEK_API_KEY 中设置");
        }
    }

    private boolean isConfidentRule(String requirement, CodegenStrategy strategy) {
        String lower = requirement.toLowerCase();
        return switch (strategy) {
            case VUE_PROJECT -> lower.contains("vue") || lower.contains("组件")
                    || lower.contains("工程") || lower.contains("待办")
                    || lower.contains("管理系统") || lower.contains("后台");
            case HTML_PAGE -> lower.contains("html") || lower.contains("页面")
                    || lower.contains("计算器") || lower.contains("落地页") || lower.contains("网页");
            case SINGLE_CODE -> lower.contains("hello") || lower.contains("算法")
                    || lower.contains("snippet") || lower.contains("单文件");
        };
    }

    private CodegenStrategy ruleBasedRoute(String requirement) {
        String lower = requirement.toLowerCase();
        if (lower.contains("vue") || lower.contains("组件") || lower.contains("工程")
                || lower.contains("待办") || lower.contains("管理系统") || lower.contains("后台")) {
            return CodegenStrategy.VUE_PROJECT;
        }
        if (lower.contains("html") || lower.contains("页面") || lower.contains("计算器")
                || lower.contains("落地页") || lower.contains("网页")) {
            return CodegenStrategy.HTML_PAGE;
        }
        return CodegenStrategy.SINGLE_CODE;
    }

    private CodegenStrategy parseStrategy(String response) {
        if (response.contains("VUE_PROJECT")) return CodegenStrategy.VUE_PROJECT;
        if (response.contains("HTML_PAGE")) return CodegenStrategy.HTML_PAGE;
        if (response.contains("SINGLE_CODE")) return CodegenStrategy.SINGLE_CODE;
        return null;
    }
}
