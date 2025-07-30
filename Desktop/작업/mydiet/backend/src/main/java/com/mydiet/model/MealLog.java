package com.mydiet.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealLog {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String description;
    private Integer caloriesEstimate;
    
    @Enumerated(EnumType.STRING)
    private MealType mealType;
    
    @Builder.Default
    private LocalDate date = LocalDate.now();
    
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public enum MealType {
        BREAKFAST("아침"), LUNCH("점심"), DINNER("저녁"), SNACK("간식");
        
        private final String korean;
        
        MealType(String korean) {
            this.korean = korean;
        }
        
        public String getKorean() {
            return korean;
        }
    }
}
