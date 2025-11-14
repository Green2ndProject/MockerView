package com.mockerview.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class InterviewReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by", nullable = false)
    private User generatedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReportStatus status = ReportStatus.GENERATING;

    @Column(columnDefinition = "TEXT")
    private String reportContent;

    @Column(name = "summary", length = 500)
    private String summary;

    @Column(name = "total_participants")
    private Integer totalParticipants;

    @Column(name = "total_questions")
    private Integer totalQuestions;

    @Column(name = "total_answers")
    private Integer totalAnswers;

    @Column(name = "average_score")
    private Double averageScore;

    @Column(name = "highest_score")
    private Integer highestScore;

    @Column(name = "lowest_score")
    private Integer lowestScore;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    public enum ReportStatus {
        GENERATING,
        COMPLETED,
        FAILED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
