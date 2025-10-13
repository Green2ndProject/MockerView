package com.mockerview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionStatusMessage {
    private Long sessionId;
    private String status;
    private Integer questionCount;
    private Integer answerCount;
    private List<String> participants;
    private String action;
    private String userName;
    private LocalDateTime timestamp;
}