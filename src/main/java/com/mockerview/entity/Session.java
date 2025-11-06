package com.mockerview.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString(exclude = {"host", "questions"})
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    private User host;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "video_recording_url", length = 500)
    private String videoRecordingUrl;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private SessionStatus sessionStatus = SessionStatus.PLANNED;

    @Column(name = "session_type")
    @Builder.Default
    private String sessionType = "TEXT";

    @Column(name = "is_self_interview")
    @Builder.Default
    private String isSelfInterview = "N";

    @Column(name = "is_reviewable")
    @Builder.Default
    private String isReviewable = "Y";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "media_enabled")
    @Builder.Default
    private Short mediaEnabled = 2;

    @Column(name = "agora_channel")
    private String agoraChannel;

    @Column(name = "difficulty")
    private String difficulty;

    @Column(name = "category")
    private String category;

    @Column(name = "ai_enabled")
    @Builder.Default
    private Boolean aiEnabled = true;

    @Column(name = "ai_mode")
    @Builder.Default
    private String aiMode = "FULL";

    @Column(name = "ai_feedback_delay_seconds")
    @Builder.Default
    private Integer aiFeedbackDelaySeconds = 0;

    @Column(name = "allow_participants_toggle_ai")
    @Builder.Default
    private Boolean allowParticipantsToggleAi = false;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Question> questions = new ArrayList<>();

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    @Transient
    private Long answerCount;

    @Transient
    private Double avgScore;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastActivity = LocalDateTime.now();
        if (agoraChannel == null) {
            agoraChannel = "session_" + System.currentTimeMillis();
        }
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusHours(3);
        }
    }

    public enum SessionStatus {
        PLANNED, RUNNING, ENDED
    }

    public SessionStatus getStatus() {
        return sessionStatus;
    }

    public void setStatus(SessionStatus status) {
        this.sessionStatus = status;
    }
    
    public SessionStatus getSessionStatus() {
        return sessionStatus;
    }
    
    public boolean isMediaEnabled() {
        return mediaEnabled != null && mediaEnabled == 1;
    }
    
    public void setMediaEnabledBoolean(boolean enabled) {
        this.mediaEnabled = enabled ? (short) 1 : (short) 0;
    }
}