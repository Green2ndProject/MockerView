package com.mockerview.dto;

import com.mockerview.entity.FacialAnalysis;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FacialAnalysisDTO {
    private Long id;
    private Long answerId;
    private Integer smileScore;
    private Integer eyeContactScore;
    private Integer postureScore;
    private Integer confidenceScore;
    private Integer tensionLevel;
    private String detailedAnalysis;
    private String improvementSuggestions;
    private LocalDateTime createdAt;
    
    public static FacialAnalysisDTO from(FacialAnalysis entity) {
        return FacialAnalysisDTO.builder()
            .id(entity.getId())
            .answerId(entity.getAnswer().getId())
            .smileScore(entity.getSmileScore())
            .eyeContactScore(entity.getEyeContactScore())
            .postureScore(entity.getPostureScore())
            .confidenceScore(entity.getConfidenceScore())
            .tensionLevel(entity.getTensionLevel())
            .detailedAnalysis(entity.getDetailedAnalysis())
            .improvementSuggestions(entity.getImprovementSuggestions())
            .createdAt(entity.getCreatedAt())
            .build();
    }
}
