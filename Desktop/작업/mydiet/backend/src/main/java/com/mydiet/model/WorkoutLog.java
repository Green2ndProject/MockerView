package com.mydiet.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "workout_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutLog {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private WorkoutType type;
    
    private Integer duration; // 분 단위
    private Integer caloriesBurned;

    @Builder.Default
    private LocalDate date = LocalDate.now();
    
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public enum WorkoutType {
        WALKING("걷기"), RUNNING("뛰기"), CYCLING("자전거"), 
        SWIMMING("수영"), WEIGHT_TRAINING("웨이트"), YOGA("요가");
        
        private final String korean;
        
        WorkoutType(String korean) {
            this.korean = korean;
        }
        
        public String getKorean() {
            return korean;
        }
    }
}
