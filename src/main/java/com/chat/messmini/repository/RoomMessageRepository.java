package com.chat.messmini.repository;

import com.chat.messmini.entity.RoomMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomMessageRepository extends JpaRepository<RoomMessage, Long> {
    @Query("SELECT rm FROM RoomMessage rm WHERE rm.room.id = :roomId ORDER BY rm.timestamp ASC")
    List<RoomMessage> findMessagesByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT COUNT(rm) FROM RoomMessage rm WHERE rm.room.id = :roomId AND rm.isRead = false AND rm.sender.id != :userId")
    Long countUnreadMessages(@Param("roomId") Long roomId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM RoomMessage rm WHERE rm.room.id = :roomId")
    void deleteByRoomId(@Param("roomId") Long roomId);
} 