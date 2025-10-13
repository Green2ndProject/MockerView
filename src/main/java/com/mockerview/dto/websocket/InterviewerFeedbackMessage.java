package com.mockerview.dto.websocket;

import lombok.Data;

@Data
public class InterviewerFeedbackMessage {
    private Long sessionId;
    private Long answerId;
    private Long reviewerId;
    private String reviewerName;
    private Integer score;
    private String comment;
}