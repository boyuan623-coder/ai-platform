package com.chatbot.chat.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatHistoryPage {
    private List<ChatMessageVO> records;
    private Long nextCursor;
    private boolean hasMore;
}
