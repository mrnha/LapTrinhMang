package com.chat.messmini.controller;

import com.chat.messmini.dto.LoginRequest;
import com.chat.messmini.dto.RegisterRequest;
import com.chat.messmini.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthApiController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        log.info("Nhận request đăng ký cho user: {}", request.getUsername());
        try {
            boolean success = userService.register(
                request.getUsername(),
                request.getPassword(),
                request.getDisplayName()
            );
            
            if (success) {
                log.info("Đăng ký thành công cho user: {}", request.getUsername());
                return ResponseEntity.ok().body("Đăng ký thành công");
            } else {
                log.warn("Đăng ký thất bại - Tên đăng nhập đã tồn tại: {}", request.getUsername());
                return ResponseEntity.badRequest().body("Tên đăng nhập đã tồn tại");
            }
        } catch (Exception e) {
            log.error("Lỗi khi xử lý đăng ký cho user {}: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.internalServerError().body("Lỗi server: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        log.info("Nhận request đăng nhập cho user: {}", request.getUsername());
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
                )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("Đăng nhập thành công cho user: {}", request.getUsername());
            return ResponseEntity.ok().body("Đăng nhập thành công");
        } catch (Exception e) {
            log.error("Đăng nhập thất bại cho user {}: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.badRequest().body("Tên đăng nhập hoặc mật khẩu không đúng");
        }
    }
}
