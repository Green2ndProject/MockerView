package com.mockerview.dto;

import lombok.Data;

@Data
public class SelfInterviewCreateDTO {
    private String title;
    private Integer questionCount;
    private String category;
    private String difficulty;
    private Integer difficultyLevel;
    private String sessionType;
    private String questionType;
    private Boolean aiEnabled;
}
