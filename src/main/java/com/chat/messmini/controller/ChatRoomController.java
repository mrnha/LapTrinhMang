package com.chat.messmini.controller;

import com.chat.messmini.entity.ChatRoom;
import com.chat.messmini.entity.RoomMessage;
import com.chat.messmini.service.ChatRoomService;
import com.chat.messmini.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Slf4j
public class ChatRoomController {
    private final ChatRoomService chatRoomService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ChatRoom> createRoom(
            @RequestBody CreateRoomRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = ((com.chat.messmini.security.CustomUserDetails) userDetails).getId();
        return ResponseEntity.ok(chatRoomService.createRoom(
            request.getName(),
            request.getDescription(),
            userId,
            request.getMemberIds()
        ));
    }

    @PostMapping("/{roomId}/members/{userId}")
    public ResponseEntity<ChatRoom> addMember(
            @PathVariable Long roomId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(chatRoomService.addMember(roomId, userId));
    }

    @DeleteMapping("/{roomId}/members/{userId}")
    public ResponseEntity<ChatRoom> removeMember(
            @PathVariable Long roomId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(chatRoomService.removeMember(roomId, userId));
    }

    @MessageMapping("/chat.room")
    public void handleRoomMessage(@Payload RoomMessageRequest request, Principal principal) {
        try {
            log.info("Received room message request: roomId={}, content={}", request.getRoomId(), request.getContent());
            log.info("Principal: {}", principal);
            if (principal == null) {
                log.warn("Principal is null, không thể lưu tin nhắn phòng!");
                return;
            }
            String username = principal.getName();
            com.chat.messmini.entity.User user = userService.findByUsername(username);
            Long userId = user.getId();
            RoomMessage message = chatRoomService.sendMessage(request.getRoomId(), userId, request.getContent());
            log.info("Room message saved with id={}", message.getId());
            messagingTemplate.convertAndSend("/topic/room." + request.getRoomId(), message);
        } catch (Exception e) {
            log.error("Error in handleRoomMessage: {}", e.getMessage(), e);
        }
    }

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<List<RoomMessage>> getRoomMessages(@PathVariable Long roomId) {
        return ResponseEntity.ok(chatRoomService.getRoomMessages(roomId));
    }

    @GetMapping
    public ResponseEntity<List<ChatRoom>> getUserRooms(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            Long userId = ((com.chat.messmini.security.CustomUserDetails) userDetails).getId();
            return ResponseEntity.ok(chatRoomService.getUserRooms(userId));
        } catch (Exception e) {
            log.error("Error getting user rooms: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<?> deleteRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = ((com.chat.messmini.security.CustomUserDetails) userDetails).getId();
            chatRoomService.deleteRoom(roomId, userId);
            return ResponseEntity.ok(Map.of("message", "Room successfully deleted"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting room: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error deleting room: " + e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/read")
    public ResponseEntity<Void> markMessagesAsRead(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = ((com.chat.messmini.security.CustomUserDetails) userDetails).getId();
        chatRoomService.markMessagesAsRead(roomId, userId);
        return ResponseEntity.ok().build();
    }

    // Request DTOs
    public static class CreateRoomRequest {
        private String name;
        private String description;
        private Set<Long> memberIds;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Set<Long> getMemberIds() { return memberIds; }
        public void setMemberIds(Set<Long> memberIds) { this.memberIds = memberIds; }
    }

    public static class RoomMessageRequest {
        private Long roomId;
        private String content;

        // Getters and setters
        public Long getRoomId() { return roomId; }
        public void setRoomId(Long roomId) { this.roomId = roomId; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
} 