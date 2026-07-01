package com.chatbot.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbot.chat.dto.ChatHistoryPage;
import com.chatbot.chat.dto.ChatMessageRequest;
import com.chatbot.chat.dto.ChatMessageVO;
import com.chatbot.chat.entity.ChatMessage;
import com.chatbot.chat.mapper.ChatMessageMapper;
import com.chatbot.common.exception.BusinessException;
import com.chatbot.app.service.AppService;
import com.chatbot.user.entity.User;
import com.chatbot.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final ChatMessageMapper chatMessageMapper;
    private final UserService userService;
    private final AppService appService;

    public ChatMessageVO save(String token, ChatMessageRequest request) {
        User user = userService.requireUser(token);
        if (request.getAppId() == null) {
            throw new BusinessException("appId 不能为空");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new BusinessException("消息内容不能为空");
        }
        appService.requireOwnedApp(token, request.getAppId());

        ChatMessage message = new ChatMessage();
        message.setAppId(request.getAppId());
        message.setUserId(user.getId());
        message.setRole(request.getRole() != null ? request.getRole() : "USER");
        message.setContent(request.getContent());
        message.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(message);
        return toVO(message);
    }

    public ChatHistoryPage listByApp(String token, Long appId, Long cursor, int size) {
        appService.requireOwnedApp(token, appId);
        int pageSize = Math.min(Math.max(size, 1), 50);

        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getAppId, appId)
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT " + (pageSize + 1));
        if (cursor != null && cursor > 0) {
            wrapper.lt(ChatMessage::getId, cursor);
        }

        List<ChatMessage> messages = chatMessageMapper.selectList(wrapper);
        boolean hasMore = messages.size() > pageSize;
        if (hasMore) {
            messages = messages.subList(0, pageSize);
        }

        List<ChatMessageVO> records = messages.stream().map(this::toVO).toList();
        Long nextCursor = records.isEmpty() ? null : records.get(records.size() - 1).getId();

        return ChatHistoryPage.builder()
                .records(records)
                .nextCursor(hasMore ? nextCursor : null)
                .hasMore(hasMore)
                .build();
    }

    public void deleteByApp(String token, Long appId) {
        appService.requireOwnedApp(token, appId);
        User user = userService.requireUser(token);
        chatMessageMapper.delete(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getAppId, appId)
                .eq(ChatMessage::getUserId, user.getId()));
    }

    private ChatMessageVO toVO(ChatMessage message) {
        return ChatMessageVO.builder()
                .id(message.getId())
                .appId(message.getAppId())
                .role(message.getRole())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
