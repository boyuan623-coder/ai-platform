package com.chatbot.appointment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbot.appointment.dto.AssistantMessageVO;
import com.chatbot.appointment.dto.AssistantSessionVO;
import com.chatbot.appointment.entity.AssistantMessage;
import com.chatbot.appointment.entity.AssistantSession;
import com.chatbot.appointment.mapper.AssistantMessageMapper;
import com.chatbot.appointment.mapper.AssistantSessionMapper;
import com.chatbot.common.exception.BusinessException;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantHistoryService {

    public static final int RETENTION_DAYS = 30;

    private final AssistantSessionMapper sessionMapper;
    private final AssistantMessageMapper messageMapper;
    private final ChatMemoryStore chatMemoryStore;

    public List<AssistantSessionVO> listSessions(Long userId) {
        purgeExpiredForUser(userId);
        LocalDateTime since = LocalDateTime.now().minusDays(RETENTION_DAYS);
        return sessionMapper.selectList(new LambdaQueryWrapper<AssistantSession>()
                        .eq(AssistantSession::getUserId, userId)
                        .ge(AssistantSession::getUpdatedAt, since)
                        .orderByDesc(AssistantSession::getUpdatedAt))
                .stream()
                .map(this::toSessionVO)
                .toList();
    }

    public List<AssistantMessageVO> listMessages(Long userId, String sessionId) {
        requireOwnedSession(userId, sessionId);
        LocalDateTime since = LocalDateTime.now().minusDays(RETENTION_DAYS);
        return messageMapper.selectList(new LambdaQueryWrapper<AssistantMessage>()
                        .eq(AssistantMessage::getSessionId, sessionId)
                        .eq(AssistantMessage::getUserId, userId)
                        .ge(AssistantMessage::getCreatedAt, since)
                        .orderByAsc(AssistantMessage::getId))
                .stream()
                .map(this::toMessageVO)
                .toList();
    }

    @Transactional
    public AssistantSessionVO createSession(Long userId, String sessionId, String title) {
        AssistantSession session = new AssistantSession();
        session.setId(sessionId);
        session.setUserId(userId);
        session.setTitle(sanitizeTitle(title));
        LocalDateTime now = LocalDateTime.now();
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        sessionMapper.insert(session);
        return toSessionVO(session);
    }

    public AssistantSession requireOwnedSession(Long userId, String sessionId) {
        AssistantSession session = sessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getUserId())) {
            throw new BusinessException("会话不存在或无权访问");
        }
        return session;
    }

    public void ensureSession(Long userId, String sessionId, String firstMessageHint) {
        AssistantSession existing = sessionMapper.selectById(sessionId);
        if (existing != null) {
            if (!userId.equals(existing.getUserId())) {
                throw new BusinessException("会话不存在或无权访问");
            }
            return;
        }
        createSession(userId, sessionId, titleFromMessage(firstMessageHint));
    }

    public AssistantMessageVO saveMessage(String sessionId, Long userId, String role, String content) {
        requireOwnedSession(userId, sessionId);
        AssistantMessage message = new AssistantMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
        touchSession(sessionId);
        maybeUpdateTitle(sessionId, userId, role, content);
        return toMessageVO(message);
    }

    @Transactional
    public void deleteSession(Long userId, String sessionId) {
        requireOwnedSession(userId, sessionId);
        messageMapper.delete(new LambdaQueryWrapper<AssistantMessage>()
                .eq(AssistantMessage::getSessionId, sessionId)
                .eq(AssistantMessage::getUserId, userId));
        sessionMapper.deleteById(sessionId);
        chatMemoryStore.deleteMessages("appointment:" + sessionId);
    }

    public void touchSession(String sessionId) {
        AssistantSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            session.setUpdatedAt(LocalDateTime.now());
            sessionMapper.updateById(session);
        }
    }

    @Scheduled(cron = "0 30 3 * * ?")
    public void purgeExpiredScheduled() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        List<AssistantSession> expired = sessionMapper.selectList(new LambdaQueryWrapper<AssistantSession>()
                .lt(AssistantSession::getUpdatedAt, cutoff));
        for (AssistantSession session : expired) {
            messageMapper.delete(new LambdaQueryWrapper<AssistantMessage>()
                    .eq(AssistantMessage::getSessionId, session.getId()));
            sessionMapper.deleteById(session.getId());
            chatMemoryStore.deleteMessages("appointment:" + session.getId());
        }
        if (!expired.isEmpty()) {
            log.info("Purged {} expired assistant sessions", expired.size());
        }
    }

    private void purgeExpiredForUser(Long userId) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        List<AssistantSession> expired = sessionMapper.selectList(new LambdaQueryWrapper<AssistantSession>()
                .eq(AssistantSession::getUserId, userId)
                .lt(AssistantSession::getUpdatedAt, cutoff));
        for (AssistantSession session : expired) {
            messageMapper.delete(new LambdaQueryWrapper<AssistantMessage>()
                    .eq(AssistantMessage::getSessionId, session.getId()));
            sessionMapper.deleteById(session.getId());
            chatMemoryStore.deleteMessages("appointment:" + session.getId());
        }
    }

    private void maybeUpdateTitle(String sessionId, Long userId, String role, String content) {
        if (!"USER".equals(role)) {
            return;
        }
        AssistantSession session = requireOwnedSession(userId, sessionId);
        if (!"新对话".equals(session.getTitle())) {
            return;
        }
        session.setTitle(titleFromMessage(content));
        sessionMapper.updateById(session);
    }

    private String titleFromMessage(String content) {
        if (content == null || content.isBlank()) {
            return "新对话";
        }
        return sanitizeTitle(content);
    }

    private String sanitizeTitle(String text) {
        String t = text.replaceAll("\\s+", " ").trim();
        if (t.length() > 40) {
            return t.substring(0, 39) + "…";
        }
        return t.isEmpty() ? "新对话" : t;
    }

    private AssistantSessionVO toSessionVO(AssistantSession session) {
        return AssistantSessionVO.builder()
                .id(session.getId())
                .title(session.getTitle())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    private AssistantMessageVO toMessageVO(AssistantMessage message) {
        return AssistantMessageVO.builder()
                .id(message.getId())
                .sessionId(message.getSessionId())
                .role(message.getRole())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
