package com.mockerview.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationMessage {
    private String type;
    private String title;
    private String message;
    private String link;
    private LocalDateTime timestamp;
    private Object data;
}
