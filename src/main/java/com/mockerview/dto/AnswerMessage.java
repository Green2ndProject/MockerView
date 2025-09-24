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
public class AnswerMessage {
    private Long questionId;
    private Long answerId;
    private Long userId;
    private String userName;
    private String answerText;
    private Integer score;
    private LocalDateTime timestamp;
}