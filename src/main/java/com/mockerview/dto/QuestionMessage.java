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
public class QuestionMessage {
    private Long sessionId;
    private Long questionId;
    private String questionText;
    private Integer orderNo;
    private Long questionerId;
    private LocalDateTime timestamp;
}