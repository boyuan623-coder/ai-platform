package com.chatbot.common.constant;

public final class CacheKeys {

    private CacheKeys() {}

    public static final String AI_RESULT_PREFIX = "ai:result:";
    public static final String CHAT_MEMORY_PREFIX = "chat:memory:";
    public static final String EMBEDDING_PREFIX = "embedding:";

    public static String aiResult(String appId, String hash) {
        return AI_RESULT_PREFIX + appId + ":" + hash;
    }

    public static String chatMemory(String appId, String sessionId) {
        return CHAT_MEMORY_PREFIX + appId + ":" + sessionId;
    }

    public static String userToken(String token) {
        return "user:token:" + token;
    }
}
