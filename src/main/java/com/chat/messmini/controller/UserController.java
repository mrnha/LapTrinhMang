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
import java.util.Map;

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

    @PostMapping("/public-key")
    public ResponseEntity<?> uploadPublicKey(@AuthenticationPrincipal CustomUserDetails userDetails,
                                         @RequestBody Map<String, String> payload) {
        String publicKey = payload.get("publicKey");
        if (publicKey == null || publicKey.isEmpty()) {
            return ResponseEntity.badRequest().body("Public key is required");
        }
        User user = userService.findByUsername(userDetails.getUsername());
        userService.updatePublicKey(user.getId(), publicKey);
        return ResponseEntity.ok("Public key updated");
    }

    @GetMapping("/{username}/public-key")
    public ResponseEntity<?> getPublicKey(@PathVariable String username) {
        String publicKey = userService.getPublicKeyByUsername(username);
        if (publicKey == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("publicKey", publicKey));
    }
} 