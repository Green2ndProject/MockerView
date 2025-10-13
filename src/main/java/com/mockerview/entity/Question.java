package com.mockerview.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
    
    @Lob
    @Column(name = "QUESTION_TEXT", nullable = false)
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
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}