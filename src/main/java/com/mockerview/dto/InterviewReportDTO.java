package com.mockerview.dto;

import com.mockerview.entity.InterviewReport;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewReportDTO {
    private Long id;
    private Long sessionId;
    private String sessionTitle;
    private Long generatedByUserId;
    private String generatedByUserName;
    private String status;
    private String reportContent;
    private String summary;
    private Integer totalParticipants;
    private Integer totalQuestions;
    private Integer totalAnswers;
    private Double averageScore;
    private Integer highestScore;
    private Integer lowestScore;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String pdfUrl;
    private String errorMessage;

    public static InterviewReportDTO fromEntity(InterviewReport report) {
        return InterviewReportDTO.builder()
            .id(report.getId())
            .sessionId(report.getSession().getId())
            .sessionTitle(report.getSession().getTitle())
            .generatedByUserId(report.getGeneratedBy().getId())
            .generatedByUserName(report.getGeneratedBy().getName())
            .status(report.getStatus().name())
            .reportContent(report.getReportContent())
            .summary(report.getSummary())
            .totalParticipants(report.getTotalParticipants())
            .totalQuestions(report.getTotalQuestions())
            .totalAnswers(report.getTotalAnswers())
            .averageScore(report.getAverageScore())
            .highestScore(report.getHighestScore())
            .lowestScore(report.getLowestScore())
            .createdAt(report.getCreatedAt())
            .completedAt(report.getCompletedAt())
            .pdfUrl(report.getPdfUrl())
            .errorMessage(report.getErrorMessage())
            .build();
    }
}
