package com.mockerview.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "facial_analysis")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FacialAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id")
    private Answer answer;
    
    @Column(name = "smile_score")
    private Integer smileScore;
    
    @Column(name = "eye_contact_score")
    private Integer eyeContactScore;
    
    @Column(name = "posture_score")
    private Integer postureScore;
    
    @Column(name = "confidence_score")
    private Integer confidenceScore;
    
    @Column(name = "tension_level")
    private Integer tensionLevel;
    
    @Column(columnDefinition = "TEXT")
    private String detailedAnalysis;
    
    @Column(columnDefinition = "TEXT")
    private String improvementSuggestions;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
