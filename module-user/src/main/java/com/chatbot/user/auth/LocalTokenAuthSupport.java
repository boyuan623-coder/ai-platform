package com.chatbot.user.auth;

import com.chatbot.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(UserService.class)
@RequiredArgsConstructor
public class LocalTokenAuthSupport implements TokenAuthSupport {

    private final UserService userService;

    @Override
    public void requireAuth(String token) {
        userService.requireUser(token);
    }
}
