package com.chatbot.workflow;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
@Service
public class CodeGenWorkflowEngine {

    public static final int MAX_RETRIES = 2;

    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;

    private static final ThreadLocal<WorkflowStreamContext> STREAM_CTX = new ThreadLocal<>();

    @Autowired
    public CodeGenWorkflowEngine(ChatModel chatModel, StreamingChatModel streamingChatModel) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
    }

    public CodeGenState execute(String requirement) throws Exception {
        CompiledGraph<CodeGenState> graph = buildGraph().compile();
        return graph.invoke(Map.of("requirement", requirement, "retryCount", 0)).get();
    }

    public CodeGenState executeStreaming(String requirement, WorkflowStreamContext listener) throws Exception {
        STREAM_CTX.set(listener);
        try {
            CompiledGraph<CodeGenState> graph = buildGraph().compile();
            var generator = graph.stream(Map.of("requirement", requirement, "retryCount", 0));

            CodeGenState[] lastHolder = new CodeGenState[1];
            lastHolder[0] = new CodeGenState(Map.of("requirement", requirement, "retryCount", 0));

            CompletableFuture<Object> future = generator.forEachAsync(output -> {
                if (output.isSTART() || output.isEND()) {
                    return;
                }
                lastHolder[0] = output.state();
                log.info("[workflow] node {} phase={}", output.node(), lastHolder[0].phase());
                listener.onNodeComplete(output.node(), lastHolder[0]);
            });

            future.get(30, TimeUnit.MINUTES);
            return lastHolder[0];
        } finally {
            STREAM_CTX.remove();
        }
    }

    private StateGraph<CodeGenState> buildGraph() throws GraphStateException {
        return new StateGraph<>(CodeGenState.SCHEMA, CodeGenState::new)
                .addNode("understand", node_async(understandNode()))
                .addNode("plan", node_async(planNode()))
                .addNode("generate", node_async(generateNode()))
                .addNode("review", node_async(reviewNode()))
                .addNode("optimize", node_async(optimizeNode()))
                .addEdge(START, "understand")
                .addEdge("understand", "plan")
                .addEdge("plan", "generate")
                .addEdge("generate", "review")
                .addConditionalEdges("review", edge_async(reviewRouter()), Map.of(
                        "optimize", "optimize",
                        "regenerate", "generate"
                ))
                .addEdge("optimize", END);
    }

    private org.bsc.langgraph4j.action.EdgeAction<CodeGenState> reviewRouter() {
        return state -> {
            if (state.validationPassed() || state.retryCount() >= MAX_RETRIES) {
                return "optimize";
            }
            return "regenerate";
        };
    }

    private String llmChat(String nodeId, String prompt) {
        WorkflowStreamContext ctx = STREAM_CTX.get();
        try {
            if (ctx != null && streamingChatModel != null) {
                return LlmStreamHelper.chatStreamCollect(
                        streamingChatModel,
                        prompt,
                        token -> ctx.onToken(nodeId, token)
                );
            }
            return chatModel.chat(prompt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private NodeAction<CodeGenState> understandNode() {
        return state -> {
            String analysis = llmChat("understand",
                    "你是资深架构师。分析以下需求，输出功能点与技术要点（简洁）：\n" + state.requirement());
            log.info("[workflow] understand done");
            return Map.of("analysis", analysis, "phase", "UNDERSTAND");
        };
    }

    private NodeAction<CodeGenState> planNode() {
        return state -> {
            String plan = llmChat("plan", """
                    你是技术负责人。根据需求与分析，输出实现方案（步骤列表 + 代码结构说明，简洁）：
                    需求：%s
                    分析：%s
                    """.formatted(state.requirement(), state.analysis()));
            log.info("[workflow] plan done");
            return Map.of("plan", plan, "phase", "PLAN");
        };
    }

    private NodeAction<CodeGenState> generateNode() {
        return state -> {
            StringBuilder prompt = new StringBuilder();
            if (state.retryCount() > 0 && !state.reviewNotes().isBlank()) {
                prompt.append("上次代码审查未通过，请根据审查意见修正后重新生成（仅输出代码块）：\n");
                prompt.append("审查意见：").append(state.reviewNotes()).append("\n\n");
            }
            prompt.append("""
                    根据需求、分析与方案生成可运行代码（仅输出代码块）：
                    需求：%s
                    分析：%s
                    方案：%s
                    """.formatted(state.requirement(), state.analysis(), state.plan()));

            String code = llmChat("generate", prompt.toString());
            log.info("[workflow] generate done retry={}", state.retryCount());
            return Map.of("generatedCode", code, "phase", "GENERATE");
        };
    }

    private NodeAction<CodeGenState> reviewNode() {
        return state -> {
            String review = llmChat("review", """
                    你是代码审查员。审查以下代码是否满足需求、逻辑正确、可运行。
                    需求：%s
                    代码：
                    %s
                    
                    严格按以下格式回复（两行）：
                    VERDICT: PASS 或 VERDICT: FAIL
                    NOTES: 简要说明
                    """.formatted(state.requirement(), state.generatedCode()));

            boolean passed = review.toUpperCase().contains("VERDICT: PASS")
                    || review.toUpperCase().contains("VERDICT:PASS");

            Map<String, Object> updates = new HashMap<>();
            updates.put("reviewNotes", review);
            updates.put("validationPassed", passed ? "true" : "false");
            updates.put("phase", "REVIEW");

            if (!passed) {
                updates.put("retryCount", state.retryCount() + 1);
            }

            log.info("[workflow] review done passed={} retry={}", passed, state.retryCount());
            return updates;
        };
    }

    private NodeAction<CodeGenState> optimizeNode() {
        return state -> {
            String code = state.generatedCode();
            if (code == null || code.isBlank()) {
                return Map.of("optimizedCode", "", "phase", "OPTIMIZE");
            }
            String optimized = llmChat("optimize", """
                    优化以下代码的可读性与健壮性，保持功能不变，仅输出最终代码：
                    %s
                    """.formatted(code));
            log.info("[workflow] optimize done");
            return Map.of("optimizedCode", optimized, "phase", "OPTIMIZE");
        };
    }
}
