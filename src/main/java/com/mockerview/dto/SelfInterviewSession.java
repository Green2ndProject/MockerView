package com.mockerview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelfInterviewSession {
    private Long sessionId;
    private List<SelfInterviewQuestion> questions;
}