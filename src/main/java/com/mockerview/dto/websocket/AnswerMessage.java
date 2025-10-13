package com.mockerview.dto.websocket;

import lombok.Data;

@Data
public class AnswerMessage {
    private Long sessionId;
    private Long questionId;
    private Long userId;
    private String userName;
    private String answerText;
}