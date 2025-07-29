package com.mydiet.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nickname;
    
    @Column(nullable = false, unique = true)
    private String email;

    private Double weightGoal;
    private Double currentWeight;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EmotionMode emotionMode = EmotionMode.FRIENDLY;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public enum EmotionMode {
        RUTHLESS("무자비"), 
        TSUNDERE("츤데레"), 
        FRIENDLY("다정함"), 
        MOTIVATIONAL("동기부여"), 
        SARCASTIC("비꼬기");
        
        private final String korean;
        
        EmotionMode(String korean) {
            this.korean = korean;
        }
        
        public String getKorean() {
            return korean;
        }
    }
}
