package com.chatbot.codegen.service;

import com.chatbot.cache.MultiLevelCache;
import com.chatbot.common.constant.AppConstants;
import com.chatbot.common.constant.CacheKeys;
import com.chatbot.workflow.CodeGenState;
import com.chatbot.workflow.CodeGenWorkflowEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CodeGenService {

    private final CodeGenWorkflowEngine workflowEngine;
    private final MultiLevelCache multiLevelCache;

    public CodeGenState generate(String requirement) throws Exception {
        String cacheKey = CacheKeys.aiResult(AppConstants.APP_CODEGEN, hash(requirement));
        String cached = multiLevelCache.getOrLoad(cacheKey, Duration.ofHours(1), () -> null);
        if (cached != null) {
            return new CodeGenState(Map.of(
                    "requirement", requirement,
                    "optimizedCode", cached,
                    "phase", "CACHE_HIT"
            ));
        }

        CodeGenState result = workflowEngine.execute(requirement);
        if (result.optimizedCode() != null && !result.optimizedCode().isBlank()) {
            multiLevelCache.put(cacheKey, result.optimizedCode(), Duration.ofHours(1));
        }
        return result;
    }

    public Flux<String> generateStream(String requirement) {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        Thread.startVirtualThread(() -> {
            try {
                sink.tryEmitNext("[phase:UNDERSTAND] 正在分析需求...\n");
                CodeGenState result = generate(requirement);
                sink.tryEmitNext("[phase:" + result.phase() + "] 分析完成\n");
                if (result.analysis() != null) {
                    sink.tryEmitNext("[analysis]\n" + result.analysis() + "\n");
                }
                if (result.generatedCode() != null) {
                    sink.tryEmitNext("[code]\n" + result.generatedCode() + "\n");
                }
                if (result.optimizedCode() != null) {
                    sink.tryEmitNext("[optimized]\n" + result.optimizedCode() + "\n");
                }
                sink.tryEmitNext("[done]\n");
                sink.tryEmitComplete();
            } catch (Exception e) {
                sink.tryEmitError(e);
            }
        });

        return sink.asFlux();
    }

    private String hash(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
