package com.chat.messmini.controller;

import com.chat.messmini.entity.User;
import com.chat.messmini.service.UserService;
import com.chat.messmini.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;

    @GetMapping("/current")
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("Getting current user info for: {}", userDetails.getUsername());
        User user = userService.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(user);
    }

    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String query) {
        log.info("Searching users with query: {}", query);
        List<User> users = userService.searchUsers(query);
        log.info("Found {} users", users.size());
        return ResponseEntity.ok(users);
    }
} 