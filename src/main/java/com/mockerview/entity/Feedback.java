package com.mockerview.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedbacks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id")
    @JsonIgnoreProperties({"feedbacks", "user", "question"})
    private Answer answer;

    @Lob
    private String summary;

    @Lob
    private String strengths;

    @Lob
    private String weaknesses;

    @Lob
    private String improvement;

    @Builder.Default
    private String model = "GPT-4o-mini";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    @JsonIgnoreProperties({"password"})
    private User reviewer;

    @Column(name = "score")
    private Integer score;

    @Lob
    @Column(name = "reviewer_comment")
    private String reviewerComment;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "feedback_type", length = 20)
    private FeedbackType feedbackType = FeedbackType.AI;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum FeedbackType {
        AI, INTERVIEWER
    }
}