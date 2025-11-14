package com.mockerview.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "self_interview_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SelfInterviewReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String categoryCode;

    @Column(nullable = false)
    private String categoryName;

    @Column(nullable = false)
    private String questionType;

    @Column(nullable = false)
    private Integer difficulty;

    @Column(nullable = false)
    private Integer totalQuestions;

    private Double overallAvg;
    private Double textAvg;
    private Double audioAvg;
    private Double videoAvg;

    @Column(columnDefinition = "TEXT")
    private String questionsData;

    @Column(columnDefinition = "TEXT")
    private String feedbacksData;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
