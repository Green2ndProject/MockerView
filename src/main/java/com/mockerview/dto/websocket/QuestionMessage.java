package com.mockerview.dto.websocket;

import lombok.Data;

@Data
public class QuestionMessage {
    private Long sessionId;
    private String text;
    private Integer orderNo;
    private Integer timerSeconds;
}