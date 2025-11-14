package com.mockerview.dto.selfpractice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {
    
    @NotBlank(message = "질문은 필수입니다")
    private String questionText;
    
    @NotBlank(message = "답변은 필수입니다")
    private String answerText;
    
    @NotBlank(message = "카테고리는 필수입니다")
    private String category;
}
