package com.chatbot.codegen.service;

import com.chatbot.cache.MultiLevelCache;
import com.chatbot.common.constant.AppConstants;
import com.chatbot.common.constant.CacheKeys;
import com.chatbot.workflow.CodeGenState;
import com.chatbot.workflow.CodeGenWorkflowEngine;
import com.chatbot.workflow.WorkflowStreamContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

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
        return Flux.<String>create(sink -> startWorkflowStream(requirement, sink), FluxSink.OverflowStrategy.BUFFER);
    }

    private void startWorkflowStream(String requirement, FluxSink<String> sink) {
        Thread.startVirtualThread(() -> {
            try {
                String cacheKey = CacheKeys.aiResult(AppConstants.APP_CODEGEN, hash(requirement));
                String cached = multiLevelCache.getOrLoad(cacheKey, Duration.ofHours(1), () -> null);
                if (cached != null) {
                    safeEmit(sink, "[phase:CACHE_HIT] 命中缓存\n");
                    safeEmit(sink, "[optimized]\n" + cached + "\n");
                    safeEmit(sink, "[done]\n");
                    sink.complete();
                    return;
                }

                CodeGenState result = workflowEngine.executeStreaming(requirement, new WorkflowStreamContext() {
                    @Override
                    public void onNodeComplete(String nodeId, CodeGenState state) {
                        emitNodeProgress(sink, nodeId, state);
                    }

                    @Override
                    public void onToken(String nodeId, String token) {
                        if (token != null && !token.isEmpty()) {
                            safeEmit(sink, "[token:" + nodeId + "]" + token);
                        }
                    }
                });

                if (result.optimizedCode() != null && !result.optimizedCode().isBlank()) {
                    multiLevelCache.put(cacheKey, result.optimizedCode(), Duration.ofHours(1));
                }
                safeEmit(sink, "[done]\n");
                sink.complete();
            } catch (Exception e) {
                safeEmit(sink, "[error]\n" + e.getMessage() + "\n");
                sink.complete();
            }
        });
    }

    private void emitNodeProgress(FluxSink<String> sink, String nodeId, CodeGenState state) {
        String phase = state.phase();
        switch (nodeId) {
            case "understand" -> {
                safeEmit(sink, "[phase:UNDERSTAND] 需求理解完成\n");
                if (!state.analysis().isBlank()) {
                    safeEmit(sink, "[analysis]\n" + state.analysis() + "\n");
                }
            }
            case "plan" -> {
                safeEmit(sink, "[phase:PLAN] 方案规划完成\n");
                if (!state.plan().isBlank()) {
                    safeEmit(sink, "[plan]\n" + state.plan() + "\n");
                }
            }
            case "generate" -> {
                int retry = state.retryCount();
                String hint = retry > 0 ? "（第 " + retry + " 次重试）" : "";
                safeEmit(sink, "[phase:GENERATE] 代码生成完成" + hint + "\n");
                if (!state.generatedCode().isBlank()) {
                    safeEmit(sink, "[code]\n" + state.generatedCode() + "\n");
                }
            }
            case "review" -> {
                String verdict = state.validationPassed() ? "审查通过" : "审查未通过";
                safeEmit(sink, "[phase:REVIEW] " + verdict + "\n");
                if (!state.reviewNotes().isBlank()) {
                    safeEmit(sink, "[review]\n" + state.reviewNotes() + "\n");
                }
            }
            case "optimize" -> {
                safeEmit(sink, "[phase:OPTIMIZE] 结果优化完成\n");
                if (!state.optimizedCode().isBlank()) {
                    safeEmit(sink, "[optimized]\n" + state.optimizedCode() + "\n");
                }
            }
            default -> safeEmit(sink, "[phase:" + phase + "] 节点 " + nodeId + " 完成\n");
        }
    }

    private static void safeEmit(FluxSink<String> sink, String chunk) {
        if (!sink.isCancelled()) {
            sink.next(chunk);
        }
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
