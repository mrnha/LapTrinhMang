package com.chat.messmini.repository;

import com.chat.messmini.entity.Friendship;
import com.chat.messmini.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    
    Optional<Friendship> findBySenderAndReceiver(User sender, User receiver);
    
    boolean existsBySenderIdAndReceiverId(Long senderId, Long receiverId);
    
    boolean existsBySenderIdAndReceiverIdAndStatus(Long senderId, Long receiverId, Friendship.Status status);
    
    List<Friendship> findByReceiverIdAndStatus(Long receiverId, Friendship.Status status);
    
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f " +
           "WHERE (f.sender.id = :userId1 AND f.receiver.id = :userId2) " +
           "OR (f.sender.id = :userId2 AND f.receiver.id = :userId1)")
    boolean existsFriendshipBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
    
    @Query("SELECT f FROM Friendship f " +
           "WHERE f.receiver.id = :userId " +
           "AND f.status = 'PENDING' " +
           "ORDER BY f.createdAt DESC")
    List<Friendship> findPendingRequestsByReceiverId(@Param("userId") Long userId);
    
    @Query("SELECT DISTINCT u FROM User u " +
           "WHERE u.id IN (" +
           "   SELECT CASE " +
           "       WHEN f.userId = :userId THEN f.friendId " +
           "       WHEN f.friendId = :userId THEN f.userId " +
           "   END " +
           "   FROM Friendship f " +
           "   WHERE (f.userId = :userId OR f.friendId = :userId) " +
           "   AND f.status = 'ACCEPTED'" +
           ")")
    List<User> findFriendsByUser(@Param("user") User user);
    
    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.sender = :user1 AND f.receiver = :user2) OR " +
           "(f.sender = :user2 AND f.receiver = :user1)")
    Optional<Friendship> findByUsers(@Param("user1") User user1, @Param("user2") User user2);
    
    List<Friendship> findBySenderIdOrReceiverId(Long userId1, Long userId2);
    
    @Modifying
    @Query("DELETE FROM Friendship f WHERE (f.sender.id = ?1 AND f.receiver.id = ?2) OR (f.sender.id = ?2 AND f.receiver.id = ?1)")
    void deleteBySenderIdAndReceiverId(Long userId1, Long userId2);
} 