package com.mockerview.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "QUESTIONS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString(exclude = {"session", "questioner", "answers"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SESSION_ID")
    @JsonIgnoreProperties({"questions", "host"})
    private Session session;
    
    @Column(name = "QUESTION_TEXT", nullable = false, columnDefinition = "TEXT")
    private String text;
    
    @Column(name = "ORDER_NO")
    @Builder.Default
    private Integer orderNo = 1;
    
    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;
    
    @Column(name = "TIMER")
    private Integer timer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "QUESTIONER_ID")
    @JsonIgnoreProperties({"password"})
    private User questioner;
    
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL)
    @Builder.Default
    @JsonIgnoreProperties({"question"})
    private List<Answer> answers = new ArrayList<>();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
    
    @Column(name = "difficulty_level")
    @Builder.Default
    private Integer difficultyLevel = 2;
    
    @Column(name = "question_type")
    @Builder.Default
    private String questionType = "TECHNICAL";
    
    @Column(name = "is_ai_generated")
    @Builder.Default
    private Boolean isAiGenerated = false;
    
    @Column(name = "ai_prompt_hash")
    private String aiPromptHash;
    
    @Column(name = "tags")
    private String tags;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public String getContent() {
        return text;
    }
    
    public Integer getQuestionOrder() {
        return orderNo;
    }
}