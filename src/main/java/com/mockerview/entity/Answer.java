package com.mockerview.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "answers")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Answer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonIgnoreProperties({"answers", "session"})
    private Question question;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password"})
    private User user;
    
    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;
    
    @Column(name = "audio_url", columnDefinition = "TEXT")
    private String audioUrl;
    
    @Column(name = "video_url", columnDefinition = "TEXT")
    private String videoUrl;
    
    @Column(name = "score")
    private Integer score;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "ai_feedback_requested")
    @Builder.Default
    private Boolean aiFeedbackRequested = true;

    @Column(name = "ai_feedback_generated")
    @Builder.Default
    private Boolean aiFeedbackGenerated = false;

    @Column(name = "ai_feedback_skipped_reason")
    private String aiFeedbackSkippedReason;

    @Column(name = "ai_processing_time_ms")
    private Long aiProcessingTimeMs;

    @OneToMany(mappedBy = "answer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnoreProperties({"answer"})
    private List<Feedback> feedbacks = new ArrayList<>();
}