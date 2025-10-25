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
public class SessionJoinMessage {
    private Long sessionId;
    private Long userId;
    private String userName;
    private String action;
    private LocalDateTime timestamp;
}