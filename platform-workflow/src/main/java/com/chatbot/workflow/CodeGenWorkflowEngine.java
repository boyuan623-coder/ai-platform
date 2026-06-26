package com.chatbot.workflow;

import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Service;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * LangGraph4j 多节点工作流：需求理解 → 代码生成 → 结果优化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeGenWorkflowEngine {

    private final ChatModel chatModel;

    public CodeGenState execute(String requirement) throws Exception {
        CompiledGraph<CodeGenState> graph = buildGraph().compile();
        return graph.invoke(Map.of("requirement", requirement)).get();
    }

    private StateGraph<CodeGenState> buildGraph() throws GraphStateException {
        return new StateGraph<>(CodeGenState.SCHEMA, CodeGenState::new)
                .addNode("understand", node_async(understandNode()))
                .addNode("generate", node_async(generateNode()))
                .addNode("optimize", node_async(optimizeNode()))
                .addEdge(START, "understand")
                .addEdge("understand", "generate")
                .addEdge("generate", "optimize")
                .addEdge("optimize", END);
    }

    private NodeAction<CodeGenState> understandNode() {
        return state -> {
            String analysis = chatModel.chat(
                    "你是资深架构师。分析以下需求，输出功能点与技术要点（简洁）：\n" + state.requirement()
            );
            log.info("[workflow] understand done");
            return Map.of("analysis", analysis, "phase", "UNDERSTAND");
        };
    }

    private NodeAction<CodeGenState> generateNode() {
        return state -> {
            String prompt = """
                    根据需求与分析生成可运行代码（仅输出代码块）：
                    需求：%s
                    分析：%s
                    """.formatted(state.requirement(), state.analysis());
            String code = chatModel.chat(prompt);
            log.info("[workflow] generate done");
            return Map.of("generatedCode", code, "phase", "GENERATE");
        };
    }

    private NodeAction<CodeGenState> optimizeNode() {
        return state -> {
            String prompt = """
                    优化以下代码的可读性与健壮性，保持功能不变，仅输出最终代码：
                    %s
                    """.formatted(state.generatedCode());
            String optimized = chatModel.chat(prompt);
            log.info("[workflow] optimize done");
            return Map.of("optimizedCode", optimized, "phase", "OPTIMIZE");
        };
    }
}
