package com.mydiet.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nickname;
    private String email;
    private Double weightGoal;
    private String emotionMode; // 예: 무자비, 츤데레, 다정함

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 기본 생성자
    public User() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public Double getWeightGoal() { return weightGoal; }
    public void setWeightGoal(Double weightGoal) { this.weightGoal = weightGoal; }
    
    public String getEmotionMode() { return emotionMode; }
    public void setEmotionMode(String emotionMode) { this.emotionMode = emotionMode; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
