package com.mockerview.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionLimitResponseDTO {
    private Boolean limitReached;
    private Boolean canCreate;
    private Integer usedSessions;
    private Integer sessionLimit;
    private String currentPlan;
    private String message;
    private String upgradeUrl;
}