package com.chat.messmini.repository;

import com.chat.messmini.entity.ChatRoom;
import com.chat.messmini.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Query("SELECT cr FROM ChatRoom cr JOIN cr.members m WHERE m = :user")
    List<ChatRoom> findRoomsByUser(@Param("user") User user);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.creator = :user")
    List<ChatRoom> findRoomsCreatedByUser(@Param("user") User user);
} 