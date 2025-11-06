package com.mockerview.dto;

import com.mockerview.entity.VoiceAnalysis;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoiceAnalysisDTO {
    private Long id;
    private Long answerId;
    private Double speakingSpeed;
    private Integer fillerWordsCount;
    private Integer pauseCount;
    private Double avgPauseDuration;
    private Integer voiceStability;
    private Integer pronunciationScore;
    private String fillerWordsDetail;
    private String improvementSuggestions;
    private LocalDateTime createdAt;
    
    public static VoiceAnalysisDTO from(VoiceAnalysis entity) {
        return VoiceAnalysisDTO.builder()
            .id(entity.getId())
            .answerId(entity.getAnswer().getId())
            .speakingSpeed(entity.getSpeakingSpeed())
            .fillerWordsCount(entity.getFillerWordsCount())
            .pauseCount(entity.getPauseCount())
            .avgPauseDuration(entity.getAvgPauseDuration())
            .voiceStability(entity.getVoiceStability())
            .pronunciationScore(entity.getPronunciationScore())
            .fillerWordsDetail(entity.getFillerWordsDetail())
            .improvementSuggestions(entity.getImprovementSuggestions())
            .createdAt(entity.getCreatedAt())
            .build();
    }
}
