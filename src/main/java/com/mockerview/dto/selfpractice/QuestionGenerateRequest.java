package com.mockerview.dto.selfpractice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionGenerateRequest {
    
    @NotBlank(message = "카테고리는 필수입니다")
    private String category;
    
    @NotBlank(message = "난이도는 필수입니다")
    private String difficulty;
    
    @Min(value = 1, message = "질문 개수는 최소 1개입니다")
    @Max(value = 20, message = "질문 개수는 최대 20개입니다")
    private Integer count;
}
