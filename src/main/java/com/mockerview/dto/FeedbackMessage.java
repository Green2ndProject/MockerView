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
public class FeedbackMessage {
    private Long sessionId;
    private Long answerId;
    private Long userId;
    private Integer score;
    private String summary;
    private String strengths;
    private String weaknesses;
    private String improvement;
    private String improvements;
    private String model;
    private LocalDateTime timestamp;
}