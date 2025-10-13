package com.mockerview.dto.websocket;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SessionParticipantInfo {
    private Long userId;
    private String userName;
    private LocalDateTime joinedAt;
}