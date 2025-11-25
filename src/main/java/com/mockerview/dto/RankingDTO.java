package com.mockerview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingDTO {
    private int rank;
    private String name;
    private String stats;
    private String score;
    private boolean isCurrentUser;
}
