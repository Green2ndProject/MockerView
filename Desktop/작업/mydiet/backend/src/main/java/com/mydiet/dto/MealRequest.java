package com.mydiet.dto;

import java.time.LocalDate;

public class MealRequest {
    private Long userId;
    private String description;
    private String photoUrl;
    private Integer caloriesEstimate;
    private LocalDate date;

    // 기본 생성자
    public MealRequest() {}

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    
    public Integer getCaloriesEstimate() { return caloriesEstimate; }
    public void setCaloriesEstimate(Integer caloriesEstimate) { this.caloriesEstimate = caloriesEstimate; }
    
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}
