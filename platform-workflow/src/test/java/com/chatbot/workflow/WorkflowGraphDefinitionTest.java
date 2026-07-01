package com.chatbot.workflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowGraphDefinitionTest {

    @Test
    void nodes_containsFivePhases() {
        assertEquals(5, WorkflowGraphDefinition.nodes().size());
        assertTrue(WorkflowGraphDefinition.nodes().stream().anyMatch(n -> "review".equals(n.id())));
    }

    @Test
    void edges_hasConditionalRegenerate() {
        boolean hasRegenerate = WorkflowGraphDefinition.edges().stream()
                .anyMatch(e -> "generate".equals(e.to()) && e.condition() != null);
        assertTrue(hasRegenerate);
    }

    @Test
    void asMap_includesMaxRetries() {
        var map = WorkflowGraphDefinition.asMap();
        assertEquals(CodeGenWorkflowEngine.MAX_RETRIES, map.get("maxRetries"));
    }
}
