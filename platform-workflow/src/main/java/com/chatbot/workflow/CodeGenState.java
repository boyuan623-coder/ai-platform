package com.chatbot.workflow;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.Map;

public class CodeGenState extends AgentState {

    static final Map<String, Channel<?>> SCHEMA = Map.of(
            "requirement", Channels.base(() -> ""),
            "analysis", Channels.base(() -> ""),
            "plan", Channels.base(() -> ""),
            "generatedCode", Channels.base(() -> ""),
            "reviewNotes", Channels.base(() -> ""),
            "validationPassed", Channels.base(() -> "false"),
            "retryCount", Channels.base(() -> 0),
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

    public String plan() {
        return this.<String>value("plan").orElse("");
    }

    public String generatedCode() {
        return this.<String>value("generatedCode").orElse("");
    }

    public String reviewNotes() {
        return this.<String>value("reviewNotes").orElse("");
    }

    public boolean validationPassed() {
        return "true".equalsIgnoreCase(this.<String>value("validationPassed").orElse("false"));
    }

    public int retryCount() {
        return this.<Integer>value("retryCount").orElse(0);
    }

    public String optimizedCode() {
        return this.<String>value("optimizedCode").orElse("");
    }

    public String phase() {
        return this.<String>value("phase").orElse("INIT");
    }
}
