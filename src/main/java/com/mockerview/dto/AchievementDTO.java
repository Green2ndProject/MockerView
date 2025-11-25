package com.mockerview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementDTO {
    private String icon;
    private String name;
    private String description;
    private boolean earned;
    private int progress;
    private int target;
}
