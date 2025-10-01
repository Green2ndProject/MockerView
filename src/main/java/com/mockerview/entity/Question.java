package com.mockerview.entity;

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
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;

    @Lob
    @Column(nullable = false)
    private String text;

    @Column(name = "order_no")
    @Builder.Default
    private Integer orderNo = 1;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "timer")
    private Integer timer; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questioner_id")
    private User questioner;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Answer> answers = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}