package com.chat.messmini.service;

import com.chat.messmini.entity.Friendship;
import com.chat.messmini.entity.User;
import com.chat.messmini.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserService userService;

    @Transactional
    public void sendFriendRequest(User sender, User receiver) {
        log.info("Sending friend request from {} to {}", sender.getUsername(), receiver.getUsername());
        
        if (friendshipRepository.existsBySenderIdAndReceiverIdAndStatus(sender.getId(), receiver.getId(), Friendship.Status.PENDING)) {
            throw new IllegalArgumentException("A pending friend request already exists");
        }

        if (existsFriendshipBetweenUsers(sender.getId(), receiver.getId())) {
            throw new IllegalArgumentException("Friendship already exists");
        }

        friendshipRepository.deleteBySenderIdAndReceiverId(sender.getId(), receiver.getId());

        Friendship friendship = new Friendship();
        friendship.setSender(sender);
        friendship.setReceiver(receiver);
        friendship.setStatus(Friendship.Status.PENDING);
        friendship.setCreatedAt(LocalDateTime.now());
        friendshipRepository.save(friendship);
        log.info("Friend request sent successfully");
    }

    @Transactional
    public void acceptFriendRequest(Long requestId, Long receiverId) {
        Friendship friendship = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));

        if (!friendship.getReceiver().getId().equals(receiverId)) {
            throw new IllegalArgumentException("You are not authorized to accept this request");
        }

        if (friendship.getStatus() != Friendship.Status.PENDING) {
            throw new IllegalArgumentException("Friend request is not pending");
        }

        // Cập nhật status và thông tin friendship
        friendship.setStatus(Friendship.Status.ACCEPTED);
        friendship.setUserId(friendship.getSender().getId());
        friendship.setFriendId(friendship.getReceiver().getId());
        friendship.setUpdatedAt(LocalDateTime.now());
        friendshipRepository.save(friendship);

        // Tạo friendship ngược lại
        Friendship reverseFriendship = new Friendship();
        reverseFriendship.setSender(friendship.getReceiver());
        reverseFriendship.setReceiver(friendship.getSender());
        reverseFriendship.setStatus(Friendship.Status.ACCEPTED);
        reverseFriendship.setUserId(friendship.getReceiver().getId());
        reverseFriendship.setFriendId(friendship.getSender().getId());
        reverseFriendship.setCreatedAt(LocalDateTime.now());
        reverseFriendship.setUpdatedAt(LocalDateTime.now());
        friendshipRepository.save(reverseFriendship);

        log.info("Friend request accepted: {} and {} are now friends", 
            friendship.getSender().getUsername(), 
            friendship.getReceiver().getUsername());
    }

    @Transactional
    public void rejectFriendRequest(Long requestId, Long receiverId) {
        Friendship friendship = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));

        if (!friendship.getReceiver().getId().equals(receiverId)) {
            throw new IllegalArgumentException("You are not authorized to reject this request");
        }

        if (friendship.getStatus() != Friendship.Status.PENDING) {
            throw new IllegalArgumentException("Friend request is not pending");
        }

        friendship.setStatus(Friendship.Status.REJECTED);
        friendship.setUpdatedAt(LocalDateTime.now());
        friendshipRepository.save(friendship);
        log.info("Friend request rejected: {} rejected request from {}", 
            friendship.getReceiver().getUsername(), 
            friendship.getSender().getUsername());
    }

    @Transactional(readOnly = true)
    public List<Friendship> getFriendRequests(Long userId) {
        log.info("Getting friend requests for user {}", userId);
        List<Friendship> requests = friendshipRepository.findByReceiverIdAndStatus(userId, Friendship.Status.PENDING);
        log.info("Found {} pending friend requests for user {}", requests.size(), userId);
        return requests;
    }

    @Transactional(readOnly = true)
    public List<User> getFriends(Long userId) {
        return getFriendsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public boolean areFriends(Long userId1, Long userId2) {
        return friendshipRepository.existsBySenderIdAndReceiverIdAndStatus(userId1, userId2, Friendship.Status.ACCEPTED) ||
               friendshipRepository.existsBySenderIdAndReceiverIdAndStatus(userId2, userId1, Friendship.Status.ACCEPTED);
    }

    @Transactional(readOnly = true)
    public boolean hasPendingRequest(Long senderId, Long receiverId) {
        return friendshipRepository.existsBySenderIdAndReceiverIdAndStatus(senderId, receiverId, Friendship.Status.PENDING);
    }

    public List<User> findFriendsByUser(User user) {
        return getFriendsByUserId(user.getId());
    }

    public List<User> getFriendsByUserId(Long userId) {
        log.info("Getting friends for user {}", userId);
        List<Friendship> friendships = friendshipRepository.findBySenderIdOrReceiverId(userId, userId);
        List<User> friends = new ArrayList<>();
        
        for (Friendship friendship : friendships) {
            if (friendship.getStatus() == Friendship.Status.ACCEPTED) {
                // Nếu userId là người gửi, thêm người nhận vào danh sách bạn bè
                if (friendship.getSender().getId().equals(userId)) {
                    friends.add(friendship.getReceiver());
                }
                // Nếu userId là người nhận, thêm người gửi vào danh sách bạn bè
                else if (friendship.getReceiver().getId().equals(userId)) {
                    friends.add(friendship.getSender());
                }
            }
        }
        
        log.info("Found {} friends for user {}", friends.size(), userId);
        return friends;
    }

    public List<User> getFriendsByUsername(String username) {
        User user = userService.findByUsername(username);
        if (user == null) {
            return List.of();
        }
        return getFriendsByUserId(user.getId());
    }

    public List<Friendship> getPendingRequestsByReceiverId(Long receiverId) {
        log.info("Getting pending friend requests for user {}", receiverId);
        List<Friendship> requests = friendshipRepository.findByReceiverIdAndStatus(receiverId, Friendship.Status.PENDING);
        log.info("Found {} pending requests for user {}", requests.size(), receiverId);
        return requests;
    }

    public boolean existsFriendshipBetweenUsers(Long userId1, Long userId2) {
        return friendshipRepository.existsBySenderIdAndReceiverIdAndStatus(userId1, userId2, Friendship.Status.ACCEPTED) ||
               friendshipRepository.existsBySenderIdAndReceiverIdAndStatus(userId2, userId1, Friendship.Status.ACCEPTED);
    }

    @Transactional
    public void unfriend(Long userId, Long friendId) {
        log.info("Unfriending users {} and {}", userId, friendId);
        
        if (!existsFriendshipBetweenUsers(userId, friendId)) {
            throw new IllegalArgumentException("No friendship exists between these users");
        }

        // Delete both friendship records
        friendshipRepository.deleteBySenderIdAndReceiverId(userId, friendId);
        log.info("Successfully unfriended users {} and {}", userId, friendId);
    }
} 