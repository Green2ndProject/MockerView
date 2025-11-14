package com.mockerview.controller.api;

import com.mockerview.dto.QuestionPoolStatsDTO;
import com.mockerview.service.QuestionPoolLearningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/analytics/questionpool")
@RequiredArgsConstructor
public class QuestionPoolAnalyticsController {

    private final QuestionPoolLearningService learningService;

    @GetMapping("/stats")
    public ResponseEntity<QuestionPoolStatsDTO> getDetailedStats() {
        long poolSize = learningService.getQuestionPoolSize();
        double aiUsageRate = learningService.getAiUsageRate();
        double cacheHitRate = 100 - aiUsageRate;
        
        QuestionPoolStatsDTO stats = QuestionPoolStatsDTO.builder()
            .totalQuestions(poolSize)
            .poolQuestions(poolSize)
            .aiUsageRate(aiUsageRate)
            .cacheHitRate(cacheHitRate)
            .efficiencyStatus(getEfficiencyStatus(poolSize))
            .maturityLevel(getMaturityLevel(poolSize))
            .message(getEfficiencyMessage(poolSize, aiUsageRate))
            .costSavings(calculateCostSavings(aiUsageRate, poolSize))
            .growthMetrics(estimateGrowthMetrics(poolSize))
            .performanceMetrics(calculatePerformanceMetrics(cacheHitRate))
            .build();
        
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/force-learning")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> forceLearning() {
        long beforeSize = learningService.getQuestionPoolSize();
        double beforeAiRate = learningService.getAiUsageRate();
        
        log.info("🔧 [관리자 강제 학습 시작]");
        
        learningService.analyzeAndLearn();
        
        long afterSize = learningService.getQuestionPoolSize();
        double afterAiRate = learningService.getAiUsageRate();
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        result.put("before", Map.of(
            "poolSize", beforeSize,
            "aiUsageRate", beforeAiRate
        ));
        result.put("after", Map.of(
            "poolSize", afterSize,
            "aiUsageRate", afterAiRate
        ));
        result.put("improvement", Map.of(
            "newQuestions", afterSize - beforeSize,
            "aiRateReduction", beforeAiRate - afterAiRate,
            "costSavings", (beforeAiRate - afterAiRate) * 10 * 0.15
        ));
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/efficiency-proof")
    public ResponseEntity<Map<String, Object>> getEfficiencyProof() {
        long poolSize = learningService.getQuestionPoolSize();
        double aiUsageRate = learningService.getAiUsageRate();
        
        Map<String, Object> proof = new HashMap<>();
        
        proof.put("thesis", "AI가 똑똑해지는 게 아니라 시스템이 효율화된다");
        
        proof.put("evidence", Map.of(
            "questionPoolSize", poolSize,
            "aiCallReduction", String.format("%.1f%%", 100 - aiUsageRate),
            "systemEfficiency", String.format("%.1f%%", 100 - aiUsageRate),
            "costReduction", String.format("$%.2f/month", (100 - aiUsageRate) * 10 * 0.15)
        ));
        
        proof.put("mechanism", Arrays.asList(
            "1. 사용자 답변 누적 → 데이터 수집",
            "2. 답변 5개 이상 + 평균 70점 이상 → 검증",
            "3. 검증된 질문 → QuestionPool 저장",
            "4. 새 질문 요청 시 QuestionPool 우선 사용",
            "5. AI API 호출 감소 → 비용 절감"
        ));
        
        proof.put("continuousLearning", Map.of(
            "autoScheduler", "매일 자정 자동 학습",
            "manualTrigger", "/api/analytics/questionpool/force-learning",
            "feedbackLoop", "답변 → 분석 → 저장 → 재사용 → 효율화"
        ));
        
        proof.put("metrics", Map.of(
            "initialState", "AI 사용률 100% (모든 질문 생성)",
            "currentState", String.format("AI 사용률 %.1f%% (캐시 히트 %.1f%%)", aiUsageRate, 100 - aiUsageRate),
            "targetState", "AI 사용률 30% (캐시 히트 70%)",
            "maturityLevel", getMaturityLevel(poolSize)
        ));
        
        proof.put("benefits", Arrays.asList(
            "✅ AI 비용 절감 (GPT-4o-mini 호출 감소)",
            "✅ 응답 속도 향상 (DB 조회 < API 호출)",
            "✅ 질문 품질 보장 (검증된 질문만 저장)",
            "✅ 시스템 자동 최적화 (사용할수록 효율 증가)"
        ));
        
        return ResponseEntity.ok(proof);
    }

    @GetMapping("/learning-curve")
    public ResponseEntity<Map<String, Object>> getLearningCurve() {
        long poolSize = learningService.getQuestionPoolSize();
        
        List<Map<String, Object>> projectedCurve = new ArrayList<>();
        
        projectedCurve.add(createCurvePoint(0, 100.0, 0, "초기 상태"));
        projectedCurve.add(createCurvePoint(100, 90.0, 50, "초기 학습"));
        projectedCurve.add(createCurvePoint(500, 70.0, 200, "성장기"));
        projectedCurve.add(createCurvePoint(1000, 50.0, 500, "고도화"));
        projectedCurve.add(createCurvePoint(5000, 30.0, 2000, "성숙기"));
        
        Map<String, Object> curve = new HashMap<>();
        curve.put("currentPosition", findCurrentPosition(poolSize));
        curve.put("projectedCurve", projectedCurve);
        curve.put("estimatedTime", estimateTimeToMaturity(poolSize));
        
        return ResponseEntity.ok(curve);
    }

    private QuestionPoolStatsDTO.CostSavings calculateCostSavings(double aiUsageRate, long poolSize) {
        double savedCallsRate = 100 - aiUsageRate;
        double monthlyCalls = 1000.0;
        double costPerCall = 0.15;
        
        return QuestionPoolStatsDTO.CostSavings.builder()
            .monthlySavings((savedCallsRate / 100) * monthlyCalls * costPerCall)
            .annualSavings((savedCallsRate / 100) * monthlyCalls * costPerCall * 12)
            .totalSavedCalls((savedCallsRate / 100) * monthlyCalls)
            .projectedSavings(estimateProjectedSavings(poolSize))
            .build();
    }

    private QuestionPoolStatsDTO.GrowthMetrics estimateGrowthMetrics(long poolSize) {
        long dailyGrowth = Math.min(poolSize / 30, 50);
        
        return QuestionPoolStatsDTO.GrowthMetrics.builder()
            .dailyGrowth(dailyGrowth)
            .weeklyGrowth(dailyGrowth * 7)
            .monthlyGrowth(dailyGrowth * 30)
            .growthRate(calculateGrowthRate(poolSize))
            .build();
    }

    private QuestionPoolStatsDTO.PerformanceMetrics calculatePerformanceMetrics(double cacheHitRate) {
        double cacheResponseTime = 50.0;
        double aiResponseTime = 1500.0;
        double avgResponseTime = (cacheHitRate / 100) * cacheResponseTime + 
                                  ((100 - cacheHitRate) / 100) * aiResponseTime;
        double improvement = ((aiResponseTime - avgResponseTime) / aiResponseTime) * 100;
        
        return QuestionPoolStatsDTO.PerformanceMetrics.builder()
            .avgResponseTime(avgResponseTime)
            .cacheResponseTime(cacheResponseTime)
            .aiResponseTime(aiResponseTime)
            .performanceImprovement(improvement)
            .build();
    }

    private String getEfficiencyStatus(long poolSize) {
        if (poolSize >= 5000) return "최적화 완료";
        if (poolSize >= 1000) return "고도화 진행 중";
        if (poolSize >= 500) return "학습 중";
        if (poolSize >= 100) return "초기 단계";
        return "데이터 수집 중";
    }

    private String getMaturityLevel(long poolSize) {
        if (poolSize >= 5000) return "완전 성숙";
        if (poolSize >= 1000) return "고도화 단계";
        if (poolSize >= 500) return "성장 중";
        if (poolSize >= 100) return "초기 단계";
        return "데이터 수집 중";
    }

    private String getEfficiencyMessage(long poolSize, double aiUsageRate) {
        if (poolSize >= 5000) {
            return String.format("시스템이 완전히 최적화되었습니다! AI 비용이 %.0f%% 절감되었습니다.", 100 - aiUsageRate);
        } else if (poolSize >= 1000) {
            return String.format("시스템 학습이 순조롭게 진행 중입니다. 현재 %.0f%% 비용 절감!", 100 - aiUsageRate);
        } else if (poolSize >= 500) {
            return String.format("QuestionPool이 %d개 누적되었습니다. AI 의존도가 %.0f%%로 감소했습니다.", poolSize, aiUsageRate);
        } else if (poolSize >= 100) {
            return String.format("초기 학습 단계입니다. %d개의 검증된 질문이 저장되었습니다.", poolSize);
        } else {
            return "시스템이 데이터를 수집하고 학습하는 중입니다.";
        }
    }

    private double estimateProjectedSavings(long poolSize) {
        double targetPoolSize = 5000.0;
        double projectedCacheRate = Math.min((poolSize / targetPoolSize) * 70, 70);
        return (projectedCacheRate / 100) * 1000 * 0.15 * 12;
    }

    private double calculateGrowthRate(long poolSize) {
        if (poolSize == 0) return 0.0;
        return Math.min((poolSize / 5000.0) * 100, 100);
    }

    private Map<String, Object> createCurvePoint(long poolSize, double aiUsage, long daysSinceStart, String phase) {
        Map<String, Object> point = new HashMap<>();
        point.put("poolSize", poolSize);
        point.put("aiUsageRate", aiUsage);
        point.put("cacheHitRate", 100 - aiUsage);
        point.put("daysSinceStart", daysSinceStart);
        point.put("phase", phase);
        return point;
    }

    private String findCurrentPosition(long poolSize) {
        if (poolSize >= 5000) return "성숙기 (목표 달성)";
        if (poolSize >= 1000) return "고도화 단계";
        if (poolSize >= 500) return "성장기";
        if (poolSize >= 100) return "초기 학습";
        return "데이터 수집 시작";
    }

    private String estimateTimeToMaturity(long poolSize) {
        if (poolSize >= 5000) return "이미 목표 달성!";
        
        long remaining = 5000 - poolSize;
        long dailyGrowth = Math.max(poolSize / 30, 5);
        long daysNeeded = remaining / dailyGrowth;
        
        if (daysNeeded <= 30) return String.format("약 %d일 후", daysNeeded);
        if (daysNeeded <= 90) return String.format("약 %d개월 후", daysNeeded / 30);
        return String.format("약 %d개월 후", daysNeeded / 30);
    }
}
