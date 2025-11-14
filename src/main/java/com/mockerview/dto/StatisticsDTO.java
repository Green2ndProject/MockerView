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
public class StatisticsDTO {
    private int totalSessions;
    private int completedSessions;
    private int totalAnswers;
    private int totalFeedbacks;
    private double averageScore;
    private String mbtiType;
    
    private List<CategoryScoreDTO> categoryScores;
    private Map<String, Integer> monthlyProgress;
}
