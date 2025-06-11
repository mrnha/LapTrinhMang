package com.chat.messmini.dto;



import lombok.Data;

@Data
public class ChatMessage {
    private Long senderId;
    private Long receiverId;
    private String content;
}
