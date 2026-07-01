package com.chatbot.api.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRpcDTO {
    private Long id;
    private String username;
    private String nickname;
    private String role;
}
