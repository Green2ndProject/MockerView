package com.mockerview.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ReviewDTO {
    private Long id;
    private Long sessionId;
    private Long answerId;
    private Long reviewerId;
    private String reviewerName;
    private String reviewComment;
    private Double rating;
    private LocalDateTime createdAt;
}
