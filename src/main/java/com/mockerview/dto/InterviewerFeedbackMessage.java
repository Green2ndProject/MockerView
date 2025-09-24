package com.mockerview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewerFeedbackMessage {
    private Long sessionId;
    private Long answerId;
    private Long reviewerId;
    private String reviewerName;
    private Integer score;
    private String comment;
    private LocalDateTime timestamp;
}