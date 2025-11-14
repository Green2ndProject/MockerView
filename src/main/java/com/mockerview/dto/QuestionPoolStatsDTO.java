package com.mockerview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionPoolStatsDTO {
    
    private Long totalQuestions;
    private Long poolQuestions;
    private Long aiGeneratedQuestions;
    private Double aiUsageRate;
    private Double cacheHitRate;
    
    private String efficiencyStatus;
    private String maturityLevel;
    private String message;
    
    private CostSavings costSavings;
    private GrowthMetrics growthMetrics;
    private PerformanceMetrics performanceMetrics;
    
    private Map<String, CategoryStats> categoryBreakdown;
    private List<LearningEvent> recentLearningEvents;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostSavings {
        private Double monthlySavings;
        private Double annualSavings;
        private Double totalSavedCalls;
        private Double projectedSavings;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrowthMetrics {
        private Long dailyGrowth;
        private Long weeklyGrowth;
        private Long monthlyGrowth;
        private Double growthRate;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetrics {
        private Double avgResponseTime;
        private Double cacheResponseTime;
        private Double aiResponseTime;
        private Double performanceImprovement;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStats {
        private Long total;
        private Long fromPool;
        private Long fromAi;
        private Double poolUsageRate;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LearningEvent {
        private String timestamp;
        private Long questionsAdded;
        private String category;
        private Double avgScore;
        private String status;
    }
}
