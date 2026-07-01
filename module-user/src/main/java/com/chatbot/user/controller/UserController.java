package com.chatbot.user.controller;

import com.chatbot.common.api.ApiResponse;
import com.chatbot.user.dto.LoginResult;
import com.chatbot.user.dto.UserLoginRequest;
import com.chatbot.user.dto.UserRegisterRequest;
import com.chatbot.user.dto.UserVO;
import com.chatbot.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ApiResponse<UserVO> register(@RequestBody UserRegisterRequest request) {
        return ApiResponse.ok(userService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResult> login(@RequestBody UserLoginRequest request) {
        return ApiResponse.ok(userService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        userService.logout(token);
        return ApiResponse.ok(null);
    }

    @GetMapping("/me")
    public ApiResponse<UserVO> me(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        return ApiResponse.ok(userService.getCurrentUser(token));
    }
}
