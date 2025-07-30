package com.mydiet.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "meal_logs")
public class MealLog {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String description;
    private String photoUrl; // optional
    private Integer caloriesEstimate;

    private LocalDate date;

    // 기본 생성자
    public MealLog() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    
    public Integer getCaloriesEstimate() { return caloriesEstimate; }
    public void setCaloriesEstimate(Integer caloriesEstimate) { this.caloriesEstimate = caloriesEstimate; }
    
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}
