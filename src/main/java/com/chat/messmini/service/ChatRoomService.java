package com.chat.messmini.service;

import com.chat.messmini.entity.ChatRoom;
import com.chat.messmini.entity.RoomMessage;
import com.chat.messmini.entity.User;
import com.chat.messmini.repository.ChatRoomRepository;
import com.chat.messmini.repository.RoomMessageRepository;
import com.chat.messmini.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final RoomMessageRepository roomMessageRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatRoom createRoom(String name, String description, Long creatorId, Set<Long> memberIds) {
        User creator = userRepository.findById(creatorId)
            .orElseThrow(() -> new RuntimeException("Creator not found"));

        ChatRoom room = new ChatRoom();
        room.setName(name);
        room.setDescription(description);
        room.setCreator(creator);

        // Đảm bảo creator luôn là thành viên
        Set<Long> allMemberIds = new java.util.HashSet<>(memberIds);
        allMemberIds.add(creatorId);
        allMemberIds.forEach(memberId -> {
            User member = userRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found: " + memberId));
            room.getMembers().add(member);
        });

        return chatRoomRepository.save(room);
    }

    @Transactional
    public ChatRoom addMember(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("Room not found"));
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        room.getMembers().add(user);
        return chatRoomRepository.save(room);
    }

    @Transactional
    public ChatRoom removeMember(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("Room not found"));
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        room.getMembers().remove(user);
        return chatRoomRepository.save(room);
    }

    @Transactional
    public RoomMessage sendMessage(Long roomId, Long senderId, String content) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("Room not found"));
        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new RuntimeException("Sender not found"));

        // Kiểm tra xem người gửi có phải là thành viên của phòng không
        if (!room.getMembers().contains(sender)) {
            throw new RuntimeException("User is not a member of this room");
        }

        RoomMessage message = new RoomMessage();
        message.setRoom(room);
        message.setSender(sender);
        message.setContent(content);

        return roomMessageRepository.save(message);
    }

    public List<RoomMessage> getRoomMessages(Long roomId) {
        return roomMessageRepository.findMessagesByRoomId(roomId);
    }

    public List<ChatRoom> getUserRooms(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        List<ChatRoom> rooms = chatRoomRepository.findRoomsByUser(user);
        log.info("User {} (id={}) is member of {} rooms", user.getUsername(), user.getId(), rooms.size());
        return rooms;
    }

    @Transactional
    public void markMessagesAsRead(Long roomId, Long userId) {
        List<RoomMessage> unreadMessages = roomMessageRepository.findMessagesByRoomId(roomId).stream()
            .filter(message -> !message.isRead() && !message.getSender().getId().equals(userId))
            .toList();

        unreadMessages.forEach(message -> {
            message.setRead(true);
            roomMessageRepository.save(message);
        });
    }

    @Transactional
    public void deleteRoom(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        // Kiểm tra xem người xóa có phải là người tạo phòng không
        if (!room.getCreator().getId().equals(userId)) {
            throw new IllegalArgumentException("Only room creator can delete the room");
        }

        // Xóa tất cả tin nhắn trong phòng
        roomMessageRepository.deleteByRoomId(roomId);

        // Xóa phòng
        chatRoomRepository.delete(room);
        log.info("Room {} deleted by user {}", roomId, userId);
    }
} 