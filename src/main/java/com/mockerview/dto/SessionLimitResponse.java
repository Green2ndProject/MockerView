package com.mockerview.dto;

import com.mockerview.entity.Subscription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionLimitResponse {
    private boolean canCreate;
    private boolean limitReached;
    private Integer usedSessions;
    private Integer sessionLimit;
    private Subscription.PlanType currentPlan;
    private String message;
    
    public static SessionLimitResponse exceeded(Integer used, Integer limit, Subscription.PlanType plan) {
        return SessionLimitResponse.builder()
                .canCreate(false)
                .limitReached(true)
                .usedSessions(used)
                .sessionLimit(limit)
                .currentPlan(plan)
                .message("세션 생성 한도에 도달했습니다. 플랜을 업그레이드하세요.")
                .build();
    }
    
    public static SessionLimitResponse available(Integer used, Integer limit, Subscription.PlanType plan) {
        return SessionLimitResponse.builder()
                .canCreate(true)
                .limitReached(false)
                .usedSessions(used)
                .sessionLimit(limit)
                .currentPlan(plan)
                .message("세션을 생성할 수 있습니다.")
                .build();
    }
}