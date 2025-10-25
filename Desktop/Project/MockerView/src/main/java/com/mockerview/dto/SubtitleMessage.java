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
public class SubtitleMessage {
    private Long sessionId;
    private Long userId;
    private String userName;
    private String text;
    private Double confidence;
    private LocalDateTime timestamp;
    private String language;
}
