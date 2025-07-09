package com.chat.messmini.controller;

import com.chat.messmini.entity.User;
import com.chat.messmini.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {
    private final UserService userService;
    private static final String UPLOAD_DIR = "uploads/avatars";

    @GetMapping
    public String showProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("user", user);
        return "profile";
    }

    @PostMapping("/update")
    public String updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String displayName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) MultipartFile avatar,
            RedirectAttributes redirectAttributes) {
        
        try {
            User user = userService.findByUsername(userDetails.getUsername());
            
            // Update display name
            user.setDisplayName(displayName);
            
            // Update email if provided
            if (email != null && !email.trim().isEmpty()) {
                user.setEmail(email.trim());
            }
            
            // Handle avatar upload
            if (avatar != null && !avatar.isEmpty()) {
                try {
                    // Create upload directory if it doesn't exist
                    Path uploadPath = Paths.get(UPLOAD_DIR);
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }
                    
                    // Generate unique filename
                    String filename = System.currentTimeMillis() + "_" + avatar.getOriginalFilename();
                    Path filePath = uploadPath.resolve(filename);
                    
                    log.info("Saving avatar to: {}", filePath.toAbsolutePath());
                    
                    // Save file
                    Files.copy(avatar.getInputStream(), filePath);
                    
                    // Update user avatar URL
                    user.setAvatarUrl("/uploads/avatars/" + filename);
                    
                    log.info("Avatar URL set to: {}", user.getAvatarUrl());
                } catch (IOException e) {
                    log.error("Failed to save avatar: {}", e.getMessage(), e);
                    throw new IOException("Failed to save avatar: " + e.getMessage());
                }
            }
            
            userService.save(user);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
        } catch (Exception e) {
            log.error("Failed to update profile: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to update profile: " + e.getMessage());
        }
        
        return "redirect:/profile";
    }
} 