package com.mockerview.dto;

import com.mockerview.entity.InterviewMBTI;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewMBTIDTO {
    private Long id;
    private Long userId;
    private String mbtiType;
    private Integer analyticalScore;
    private Integer creativeScore;
    private Integer logicalScore;
    private Integer emotionalScore;
    private Integer detailOrientedScore;
    private Integer bigPictureScore;
    private Integer decisiveScore;
    private Integer flexibleScore;
    private String strengthDescription;
    private String weaknessDescription;
    private String careerRecommendation;
    private LocalDateTime createdAt;
    
    public static InterviewMBTIDTO from(InterviewMBTI entity) {
        return InterviewMBTIDTO.builder()
            .id(entity.getId())
            .userId(entity.getUser().getId())
            .mbtiType(entity.getMbtiType())
            .analyticalScore(entity.getAnalyticalScore())
            .creativeScore(entity.getCreativeScore())
            .logicalScore(entity.getLogicalScore())
            .emotionalScore(entity.getEmotionalScore())
            .detailOrientedScore(entity.getDetailOrientedScore())
            .bigPictureScore(entity.getBigPictureScore())
            .decisiveScore(entity.getDecisiveScore())
            .flexibleScore(entity.getFlexibleScore())
            .strengthDescription(entity.getStrengthDescription())
            .weaknessDescription(entity.getWeaknessDescription())
            .careerRecommendation(entity.getCareerRecommendation())
            .createdAt(entity.getCreatedAt())
            .build();
    }
}
