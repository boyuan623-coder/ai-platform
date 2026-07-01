package com.chatbot.userapp.rpc;

import com.chatbot.api.user.UserRpcDTO;
import com.chatbot.api.user.UserRpcService;
import com.chatbot.user.entity.User;
import com.chatbot.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService
@RequiredArgsConstructor
public class UserRpcServiceImpl implements UserRpcService {

    private final UserService userService;

    @Override
    public UserRpcDTO getUserByToken(String token) {
        try {
            User user = userService.requireUser(token);
            return UserRpcDTO.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .role(user.getRole())
                    .build();
        } catch (Exception e) {
            return null;
        }
    }
}
