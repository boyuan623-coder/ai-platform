package com.chatbot.api.user;

/**
 * Dubbo RPC：用户会话校验（微服务间调用）。
 */
public interface UserRpcService {

    UserRpcDTO getUserByToken(String token);
}
