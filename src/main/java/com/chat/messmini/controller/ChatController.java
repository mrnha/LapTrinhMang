package com.chat.messmini.controller;

import com.chat.messmini.entity.Message;
import com.chat.messmini.entity.User;
import com.chat.messmini.model.ChatMessage;
import com.chat.messmini.security.CustomUserDetails;
import com.chat.messmini.service.ChatService;
import com.chat.messmini.service.FriendshipService;
import com.chat.messmini.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final UserService userService;
    private final FriendshipService friendshipService;
    private final Map<String, Long> onlineUsersSession = new ConcurrentHashMap<>();
    private final Map<String, Boolean> userOnlineStatus = new ConcurrentHashMap<>();

    @GetMapping("/chat")
    public String chat(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("Accessing chat page with user: {}", userDetails);
            
            if (userDetails instanceof CustomUserDetails) {
                CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
                model.addAttribute("user", customUserDetails);
                model.addAttribute("username", customUserDetails.getUsername());
                model.addAttribute("displayName", customUserDetails.getDisplayName());
                
                return "chat";
            } else {
                log.error("Invalid user details type: {}", userDetails.getClass().getName());
                return "redirect:/login?error=invalid_user";
            }
        } catch (Exception e) {
            log.error("Error accessing chat page: {}", e.getMessage(), e);
            return "redirect:/login?error=server_error";
        }
    }

    @GetMapping("/api/chat/history/{userId}")
    @ResponseBody
    public List<ChatMessage> getChatHistory(@PathVariable Long userId, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Getting chat history between users: {} and {}", userDetails.getUsername(), userId);
        try {
            List<ChatMessage> messages = chatService.getChatHistory(
                ((CustomUserDetails) userDetails).getId(),
                userId
            );
            log.info("Found {} messages in chat history", messages.size());
            return messages;
        } catch (Exception e) {
            log.error("Error getting chat history: {}", e.getMessage(), e);
            throw e;
        }
    }

    @MessageMapping("/user.connected")
    public void handleUserConnected(@Payload Map<String, String> userInfo, SimpMessageHeaderAccessor headerAccessor) {
        log.info("Received user.connected event: {}", userInfo);
        Principal principal = headerAccessor.getUser();
        
        if (principal instanceof Authentication) {
            Authentication authentication = (Authentication) principal;
            if (authentication.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                Long userId = userDetails.getId();
                String sessionId = headerAccessor.getSessionId();
                
                log.info("Processing user connection - username: {}, userId: {}, sessionId: {}", 
                    userDetails.getUsername(), userId, sessionId);
                
                onlineUsersSession.put(sessionId, userId);
                chatService.userConnected(userId);
                
                // Broadcast updated user list
                List<Map<String, Object>> onlineUsers = chatService.getOnlineUsers();
                log.info("Broadcasting updated online users list: {}", onlineUsers);
                messagingTemplate.convertAndSend("/topic/users", onlineUsers);
            }
        }
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        try {
            SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
            String sessionId = headers.getSessionId();
            Principal user = headers.getUser();
            if (user == null) {
                log.warn("WebSocket connect without Principal");
                return;
            }
            String username = user.getName();
            log.info("User connected: {} with session: {}", username, sessionId);
            
            // Lấy thông tin user từ username
            User userObj = userService.findByUsername(username);
            if (userObj != null) {
                // Lưu sessionId và userId vào map
                onlineUsersSession.put(sessionId, userObj.getId());
                // Cập nhật trạng thái online
                userOnlineStatus.put(username, true);
                userObj.setOnline(true);
                userService.save(userObj);
                
                // Thông báo cho tất cả người dùng về trạng thái mới
                messagingTemplate.convertAndSend("/topic/online-users", userObj);
                
                // Broadcast danh sách người dùng online
                List<User> onlineUsers = userService.getOnlineUsers();
                messagingTemplate.convertAndSend("/topic/users", onlineUsers);
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket connect: {}", e.getMessage(), e);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        try {
            SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
            String sessionId = headers.getSessionId();
            Principal user = headers.getUser();
            if (user == null) {
                log.warn("WebSocket disconnect without Principal");
                return;
            }
            String username = user.getName();
            log.info("User disconnected: {} with session: {}", username, sessionId);
            
            // Lấy userId từ sessionId
            Long userId = onlineUsersSession.remove(sessionId);
            if (userId != null) {
                // Lấy thông tin user
                userService.findById(userId).ifPresent(userObj -> {
                    // Kiểm tra xem user còn session nào khác không
                    boolean hasOtherSessions = onlineUsersSession.values().stream()
                        .anyMatch(id -> id.equals(userId));
                    
                    if (!hasOtherSessions) {
                        // Nếu không còn session nào, cập nhật trạng thái offline
                        userOnlineStatus.remove(username);
                        userObj.setOnline(false);
                        userService.save(userObj);
                        
                        // Thông báo cho tất cả người dùng về trạng thái mới
                        messagingTemplate.convertAndSend("/topic/online-users", userObj);
                        
                        // Broadcast danh sách người dùng online
                        List<User> onlineUsers = userService.getOnlineUsers();
                        messagingTemplate.convertAndSend("/topic/users", onlineUsers);
                    }
                });
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket disconnect: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/chat.private")
    public void handlePrivateMessage(@Payload Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sender = payload.get("sender");
            String receiver = payload.get("receiver");
            String content = payload.get("content");
            
            log.info("Received private message from {} to {}: {}", sender, receiver, content);
            
            // Lưu tin nhắn vào database
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setSenderId(Long.parseLong(sender));
            chatMessage.setReceiverId(Long.parseLong(receiver));
            chatMessage.setContent(content);
            chatMessage.setTimestamp(LocalDateTime.now().toString());
            
            Message savedMessage = chatService.saveMessage(chatMessage);
            
            // Gửi tin nhắn đến người nhận
            messagingTemplate.convertAndSendToUser(
                receiver,
                "/queue/messages",
                Map.of(
                    "id", savedMessage.getId(),
                    "senderId", savedMessage.getSender().getId(),
                    "receiverId", savedMessage.getReceiver().getId(),
                    "content", savedMessage.getContent(),
                    "timestamp", savedMessage.getTimestamp().toString(),
                    "read", savedMessage.isRead()
                )
            );
            
            // Gửi tin nhắn đến người gửi (để xác nhận)
            messagingTemplate.convertAndSendToUser(
                sender,
                "/queue/messages",
                Map.of(
                    "id", savedMessage.getId(),
                    "senderId", savedMessage.getSender().getId(),
                    "receiverId", savedMessage.getReceiver().getId(),
                    "content", savedMessage.getContent(),
                    "timestamp", savedMessage.getTimestamp().toString(),
                    "read", savedMessage.isRead()
                )
            );
        } catch (Exception e) {
            log.error("Error handling private message: {}", e.getMessage(), e);
        }
    }

    @GetMapping("/api/chat/users")
    public ResponseEntity<List<User>> getUsers(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) String channelType) {
        log.info("Getting users for channel type: {}", channelType);
        
        List<User> users;
        if ("GENERAL".equals(channelType)) {
            // Trong kênh tổng hợp, hiển thị tất cả người dùng online
            users = userService.getOnlineUsers();
        } else {
            // Trong các kênh khác, chỉ hiển thị bạn bè
            users = friendshipService.getFriends(currentUser.getId());
        }
        
        return ResponseEntity.ok(users);
    }

    @GetMapping("/api/chat/online-users")
    @ResponseBody
    public ResponseEntity<List<User>> getOnlineUsers(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Getting online users for user: {}", userDetails.getUsername());
        
        try {
            if (userDetails instanceof CustomUserDetails) {
                CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
                // Lấy tất cả người dùng online
                List<User> onlineUsers = userService.getOnlineUsers();
                
                // Lọc ra những người không phải chính mình
                List<User> otherOnlineUsers = onlineUsers.stream()
                    .filter(user -> !user.getId().equals(customUserDetails.getId()))
                    .collect(Collectors.toList());
                
                log.info("Found {} online users", otherOnlineUsers.size());
                return ResponseEntity.ok(otherOnlineUsers);
            } else {
                log.error("Invalid user details type: {}", userDetails.getClass().getName());
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            log.error("Error getting online users: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @MessageMapping("/chat.addUser")
    public void addUser(@Payload Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String username = payload.get("sender");
        log.info("User connected: {}", username);
        
        // Cập nhật trạng thái online
        userOnlineStatus.put(username, true);
        
        // Gửi thông báo trạng thái online cho tất cả người dùng
        messagingTemplate.convertAndSend("/topic/public", Map.of(
            "type", "JOIN",
            "sender", username,
            "online", true
        ));
    }

    @MessageMapping("/chat.typing")
    public void sendTypingStatus(@Payload Map<String, Object> payload) {
        String sender = (String) payload.get("sender");
        String receiver = (String) payload.get("receiver");
        Boolean typing = (Boolean) payload.get("typing");
        
        log.info("Typing status from {} to {}: {}", sender, receiver, typing);
        
        messagingTemplate.convertAndSendToUser(
            receiver,
            "/queue/typing",
            Map.of(
                "sender", sender,
                "typing", typing
            )
        );
    }

    @GetMapping("/api/chat/users/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        log.info("Getting user by username: {}", username);
        User user = userService.findByUsername(username);
        if (user != null) {
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.notFound().build();
    }
}