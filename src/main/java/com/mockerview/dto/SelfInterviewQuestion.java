package com.mockerview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelfInterviewQuestion {
    private Long id;
    private String questionText;
    private Integer orderNo;
}