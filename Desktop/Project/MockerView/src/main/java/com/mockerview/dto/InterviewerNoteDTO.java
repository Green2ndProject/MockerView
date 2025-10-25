package com.mockerview.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewerNoteDTO {
    private Long id;
    private Long sessionId;
    private Long interviewerId;
    private String interviewerName;
    private Long intervieweeId;
    private String intervieweeName;
    private Integer rating;
    private String strengths;
    private String weaknesses;
    private String improvements;
    private String overallComment;
    private String notes;
    private Boolean submitted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
