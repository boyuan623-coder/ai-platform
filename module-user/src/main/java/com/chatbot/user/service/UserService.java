package com.chatbot.user.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbot.common.constant.CacheKeys;
import com.chatbot.common.exception.BusinessException;
import com.chatbot.user.dto.LoginResult;
import com.chatbot.user.dto.UserLoginRequest;
import com.chatbot.user.dto.UserRegisterRequest;
import com.chatbot.user.dto.UserVO;
import com.chatbot.user.entity.User;
import com.chatbot.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Duration TOKEN_TTL = Duration.ofDays(7);

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserVO register(UserRegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BusinessException("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new BusinessException("密码至少 6 位");
        }
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return toVO(user);
    }

    public LoginResult login(UserLoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用");
        }

        String token = IdUtil.fastSimpleUUID();
        redisTemplate.opsForValue().set(CacheKeys.userToken(token), String.valueOf(user.getId()), TOKEN_TTL);
        return LoginResult.builder().token(token).user(toVO(user)).build();
    }

    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            redisTemplate.delete(CacheKeys.userToken(token));
        }
    }

    public UserVO getCurrentUser(String token) {
        User user = requireUser(token);
        return toVO(user);
    }

    public User requireUser(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("未登录");
        }
        String userId = redisTemplate.opsForValue().get(CacheKeys.userToken(token));
        if (userId == null) {
            throw new BusinessException("登录已过期，请重新登录");
        }
        User user = userMapper.selectById(Long.parseLong(userId));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    public boolean isAdmin(User user) {
        return "ADMIN".equals(user.getRole());
    }

    private UserVO toVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .build();
    }
}
