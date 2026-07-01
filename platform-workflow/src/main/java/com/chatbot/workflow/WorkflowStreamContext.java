package com.chatbot.workflow;

/**
 * 工作流流式上下文：节点级与 Token 级回调。
 */
public interface WorkflowStreamContext {

    void onNodeComplete(String nodeId, CodeGenState state);

    default void onToken(String nodeId, String token) {
        // optional token streaming
    }
}
