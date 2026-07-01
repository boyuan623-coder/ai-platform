package com.chatbot.user.auth;

import com.chatbot.api.user.UserRpcDTO;
import com.chatbot.api.user.UserRpcService;
import com.chatbot.common.exception.BusinessException;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 微服务模式下通过 Dubbo 调用 service-user 校验 Token。
 */
@Component
@ConditionalOnMissingBean(LocalTokenAuthSupport.class)
public class DubboTokenAuthSupport implements TokenAuthSupport {

    @DubboReference(check = false)
    private UserRpcService userRpcService;

    @Override
    public void requireAuth(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("请先登录");
        }
        UserRpcDTO user = userRpcService.getUserByToken(token);
        if (user == null) {
            throw new BusinessException("登录已过期，请重新登录");
        }
    }
}
