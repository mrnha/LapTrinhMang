package com.chat.messmini.controller;

import com.chat.messmini.entity.User;
import com.chat.messmini.entity.Friendship;
import com.chat.messmini.service.FriendshipService;
import com.chat.messmini.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class FriendshipController {

    private final FriendshipService friendshipService;
    private final UserService userService;

    @GetMapping("/api/friendships/friends")
    public ResponseEntity<?> getFriends(Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userService.findByUsername(userDetails.getUsername());
            
            if (user == null) {
                log.error("User not found: {}", userDetails.getUsername());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found");
            }
            
            List<User> friends = friendshipService.getFriendsByUserId(user.getId());
            log.info("Retrieved {} friends for user {}", friends.size(), user.getUsername());
            
            return ResponseEntity.ok(friends);
        } catch (Exception e) {
            log.error("Error getting friends: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving friends: " + e.getMessage());
        }
    }

    @GetMapping("/api/friends/requests")
    public ResponseEntity<?> getFriendRequests(Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userService.findByUsername(userDetails.getUsername());
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
            
            List<Friendship> requests = friendshipService.getPendingRequestsByReceiverId(user.getId());
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            log.error("Error getting friend requests: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error getting friend requests: " + e.getMessage());
        }
    }

    @PostMapping("/api/friendships/request/{username}")
    public ResponseEntity<?> sendFriendRequest(@PathVariable String username, Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User sender = userService.findByUsername(userDetails.getUsername());
            
            if (sender == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Sender not found"));
            }
            
            User receiver = userService.findByUsername(username);
            if (receiver == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Receiver not found"));
            }
            
            if (sender.getId().equals(receiver.getId())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Cannot send friend request to yourself"));
            }
            
            try {
                friendshipService.sendFriendRequest(sender, receiver);
                return ResponseEntity.ok(Map.of("message", "Friend request sent successfully"));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
            }
        } catch (Exception e) {
            log.error("Error sending friend request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error sending friend request: " + e.getMessage()));
        }
    }

    @PostMapping("/api/friendships/accept/{requestId}")
    public ResponseEntity<?> acceptFriendRequest(@PathVariable Long requestId, Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userService.findByUsername(userDetails.getUsername());
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
            }
            
            friendshipService.acceptFriendRequest(requestId, user.getId());
            return ResponseEntity.ok(Map.of("message", "Friend request accepted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error accepting friend request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error accepting friend request: " + e.getMessage()));
        }
    }

    @PostMapping("/api/friendships/reject/{requestId}")
    public ResponseEntity<?> rejectFriendRequest(@PathVariable Long requestId, Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userService.findByUsername(userDetails.getUsername());
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
            }
            
            friendshipService.rejectFriendRequest(requestId, user.getId());
            return ResponseEntity.ok(Map.of("message", "Friend request rejected successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error rejecting friend request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error rejecting friend request: " + e.getMessage()));
        }
    }

    @PostMapping("/api/friendships/unfriend/{friendId}")
    public ResponseEntity<?> unfriendUser(@PathVariable Long friendId, Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userService.findByUsername(userDetails.getUsername());
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
            }
            
            friendshipService.unfriend(user.getId(), friendId);
            return ResponseEntity.ok(Map.of("message", "Successfully unfriended user"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error unfriending user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error unfriending user: " + e.getMessage()));
        }
    }
} 