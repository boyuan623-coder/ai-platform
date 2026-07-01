package com.chatbot.workflow;

import java.util.List;
import java.util.Map;

/**
 * LangGraph4j 工作流拓扑定义，供 API 与前端展示。
 */
public final class WorkflowGraphDefinition {

    public record NodeDef(String id, String label, String description) {}

    public record EdgeDef(String from, String to, String condition) {}

    private WorkflowGraphDefinition() {}

    public static List<NodeDef> nodes() {
        return List.of(
                new NodeDef("understand", "需求理解", "分析需求功能点与技术要点"),
                new NodeDef("plan", "方案规划", "输出实现步骤与代码结构"),
                new NodeDef("generate", "代码生成", "根据方案生成可运行代码"),
                new NodeDef("review", "代码审查", "检查正确性与完整性"),
                new NodeDef("optimize", "结果优化", "优化可读性与健壮性")
        );
    }

    public static List<EdgeDef> edges() {
        return List.of(
                new EdgeDef("START", "understand", null),
                new EdgeDef("understand", "plan", null),
                new EdgeDef("plan", "generate", null),
                new EdgeDef("generate", "review", null),
                new EdgeDef("review", "optimize", "审查通过或已达最大重试"),
                new EdgeDef("review", "generate", "审查未通过且可重试"),
                new EdgeDef("optimize", "END", null)
        );
    }

    public static Map<String, Object> asMap() {
        return Map.of(
                "name", "codegen-workflow",
                "description", "LangGraph4j 多节点代码生成工作流",
                "nodes", nodes(),
                "edges", edges(),
                "maxRetries", CodeGenWorkflowEngine.MAX_RETRIES
        );
    }
}
