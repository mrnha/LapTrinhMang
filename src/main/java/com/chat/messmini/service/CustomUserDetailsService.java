package com.chat.messmini.service;

import com.chat.messmini.entity.User;
import com.chat.messmini.repository.UserRepository;
import com.chat.messmini.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("Đang tìm user với username: {}", username);
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("Không tìm thấy user với username: {}", username);
                    return new UsernameNotFoundException("Không tìm thấy user: " + username);
                });

        log.info("Đã tìm thấy user: {}", username);
        return new CustomUserDetails(user);
    }
}
