package com.chat.messmini.service;

import com.chat.messmini.entity.Message;
import com.chat.messmini.entity.User;
import com.chat.messmini.model.ChatMessage;
import com.chat.messmini.repository.MessageRepository;
import com.chat.messmini.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final Map<Long, Set<String>> onlineUsers = new ConcurrentHashMap<>(); // Map userId to set of sessionIds

    @Transactional
    public Message saveMessage(ChatMessage chatMessage) {
        log.info("Saving message: {}", chatMessage);
        try {
            User sender = userRepository.findById(chatMessage.getSenderId())
                .orElseThrow(() -> new RuntimeException("Sender not found"));
            User receiver = userRepository.findById(chatMessage.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

            Message message = new Message();
            message.setSender(sender);
            message.setReceiver(receiver);
            message.setContent(chatMessage.getContent());
            message.setTimestamp(LocalDateTime.now());
            message.setRead(false);

            Message savedMessage = messageRepository.save(message);
            log.info("Message saved successfully with ID: {}", savedMessage.getId());
            return savedMessage;
        } catch (Exception e) {
            log.error("Error saving message: {}", e.getMessage(), e);
            throw e;
        }
    }

    public List<ChatMessage> getChatHistory(Long userId1, Long userId2) {
        log.info("Getting chat history between users: {} and {}", userId1, userId2);
        try {
            List<Message> messages = messageRepository.findChatHistory(userId1, userId2);
            List<ChatMessage> chatMessages = new ArrayList<>();
            
            for (Message message : messages) {
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setId(message.getId());
                chatMessage.setSenderId(message.getSender().getId());
                chatMessage.setReceiverId(message.getReceiver().getId());
                chatMessage.setContent(message.getContent());
                chatMessage.setTimestamp(message.getTimestamp().toString());
                chatMessage.setRead(message.isRead());
                chatMessages.add(chatMessage);
            }
            
            log.info("Found {} messages in chat history", chatMessages.size());
            return chatMessages;
        } catch (Exception e) {
            log.error("Error getting chat history: {}", e.getMessage(), e);
            throw e;
        }
    }

    public void userConnected(Long userId) {
        log.info("User connected: userId={}", userId);
        onlineUsers.computeIfAbsent(userId, k -> new HashSet<>());
        log.info("Current online users: {}", onlineUsers);
    }

    public void userDisconnected(Long userId) {
        log.info("User disconnected: userId={}", userId);
        onlineUsers.remove(userId);
        log.info("Current online users: {}", onlineUsers);
    }

    public List<Map<String, Object>> getOnlineUsers() {
        log.info("Getting online users list");
        List<Map<String, Object>> onlineUsersList = new ArrayList<>();
        
        for (Long userId : onlineUsers.keySet()) {
            userRepository.findById(userId).ifPresent(user -> {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("username", user.getUsername());
                userInfo.put("displayName", user.getDisplayName());
                userInfo.put("online", true);
                onlineUsersList.add(userInfo);
            });
        }
        
        log.info("Found {} online users", onlineUsersList.size());
        return onlineUsersList;
    }

    @Transactional
    public void markMessageAsRead(Long messageId) {
        log.info("Marking message as read: messageId={}", messageId);
        try {
            Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
            message.setRead(true);
            messageRepository.save(message);
            log.info("Message marked as read successfully");
        } catch (Exception e) {
            log.error("Error marking message as read: {}", e.getMessage(), e);
            throw e;
        }
    }
}