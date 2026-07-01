package com.chatbot.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResult {
    private String token;
    private UserVO user;
}
