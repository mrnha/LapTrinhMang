package com.chat.messmini.model;

import lombok.Data;

@Data
public class ChatMessage {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String content;
    private String timestamp;
    private boolean isRead;
} 