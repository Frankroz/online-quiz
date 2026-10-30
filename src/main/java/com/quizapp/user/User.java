package com.quizapp.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(nullable = false, length = 10)
    private String role;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected User() {}
    public User(String username, String passwordHash, String role) {
        this.username = username; this.passwordHash = passwordHash; this.role = role;
    }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
}
