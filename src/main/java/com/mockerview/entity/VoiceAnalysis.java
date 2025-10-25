package com.mockerview.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "voice_analysis")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoiceAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id")
    private Answer answer;
    
    @Column(name = "speaking_speed")
    private Double speakingSpeed;
    
    @Column(name = "filler_words_count")
    private Integer fillerWordsCount;
    
    @Column(name = "pause_count")
    private Integer pauseCount;
    
    @Column(name = "avg_pause_duration")
    private Double avgPauseDuration;
    
    @Column(name = "voice_stability")
    private Integer voiceStability;
    
    @Column(name = "pronunciation_score")
    private Integer pronunciationScore;
    
    @Column(columnDefinition = "TEXT")
    private String fillerWordsDetail;
    
    @Column(columnDefinition = "TEXT")
    private String improvementSuggestions;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
