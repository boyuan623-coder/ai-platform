package com.chatbot.workflow;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.Map;

public class CodeGenState extends AgentState {

    static final Map<String, Channel<?>> SCHEMA = Map.of(
            "requirement", Channels.base(() -> ""),
            "analysis", Channels.base(() -> ""),
            "generatedCode", Channels.base(() -> ""),
            "optimizedCode", Channels.base(() -> ""),
            "phase", Channels.base(() -> "INIT")
    );

    public CodeGenState(Map<String, Object> initData) {
        super(initData);
    }

    public String requirement() {
        return this.<String>value("requirement").orElse("");
    }

    public String analysis() {
        return this.<String>value("analysis").orElse("");
    }

    public String generatedCode() {
        return this.<String>value("generatedCode").orElse("");
    }

    public String optimizedCode() {
        return this.<String>value("optimizedCode").orElse("");
    }

    public String phase() {
        return this.<String>value("phase").orElse("INIT");
    }
}
