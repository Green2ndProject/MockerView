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
    private String password;
    private Double currentWeight;
    private Double weightGoal;
    private String emotionMode;
    private Integer height;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public User() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public Double getCurrentWeight() { return currentWeight; }
    public void setCurrentWeight(Double currentWeight) { this.currentWeight = currentWeight; }
    
    public Double getWeightGoal() { return weightGoal; }
    public void setWeightGoal(Double weightGoal) { this.weightGoal = weightGoal; }
    
    public String getEmotionMode() { return emotionMode; }
    public void setEmotionMode(String emotionMode) { this.emotionMode = emotionMode; }
    
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}