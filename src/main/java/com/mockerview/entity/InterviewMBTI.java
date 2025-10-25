package com.mockerview.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "interview_mbti")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewMBTI {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "mbti_type", length = 4)
    private String mbtiType;
    
    @Column(name = "analytical_score")
    private Integer analyticalScore;
    
    @Column(name = "creative_score")
    private Integer creativeScore;
    
    @Column(name = "logical_score")
    private Integer logicalScore;
    
    @Column(name = "emotional_score")
    private Integer emotionalScore;
    
    @Column(name = "detail_oriented_score")
    private Integer detailOrientedScore;
    
    @Column(name = "big_picture_score")
    private Integer bigPictureScore;
    
    @Column(name = "decisive_score")
    private Integer decisiveScore;
    
    @Column(name = "flexible_score")
    private Integer flexibleScore;
    
    @Column(columnDefinition = "TEXT")
    private String strengthDescription;
    
    @Column(columnDefinition = "TEXT")
    private String weaknessDescription;
    
    @Column(columnDefinition = "TEXT")
    private String careerRecommendation;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
