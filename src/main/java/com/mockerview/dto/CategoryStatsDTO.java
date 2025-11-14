package com.mockerview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStatsDTO {
    private String categoryCode;
    private String categoryName;
    private Integer count;
}
