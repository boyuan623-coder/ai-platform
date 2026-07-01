package com.chatbot.user.auth;

/**
 * 统一 Token 鉴权（单体用本地 UserService，微服务用 Dubbo）。
 */
public interface TokenAuthSupport {

    void requireAuth(String token);
}
