package com.mockerview.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnalysisCompleteMessage {
    private Long answerId;
    private Long userId;
    private String analysisType;
    private String message;
    private Object data;
}
