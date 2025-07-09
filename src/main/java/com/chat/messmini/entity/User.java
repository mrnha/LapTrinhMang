package com.chat.messmini.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    private String password;

    @Column(name = "display_name")
    private String displayName;

    @Column(unique = true)
    private String email;

    @Column(name = "avatar_url")
    private String avatarUrl;

    private boolean online;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "roles")
    @Builder.Default
    private Set<String> roles = new HashSet<>();

    @Column(name = "public_key", columnDefinition = "TEXT")
    private String publicKey;

    @PrePersist
    protected void onCreate() {
        if (displayName == null || displayName.isEmpty()) {
            displayName = username;
        }
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            avatarUrl = "/images/default-avatar.png";
        }
    }
}
