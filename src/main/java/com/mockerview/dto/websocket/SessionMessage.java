package com.mockerview.dto.websocket;

import lombok.Data;

@Data
public class SessionMessage {
    private Long sessionId;
    private Long userId;
    private String userName;
    private String action;
}