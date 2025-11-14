package com.mockerview.scheduler;

import com.mockerview.service.QuestionPoolLearningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionPoolLearningScheduler {

    private final QuestionPoolLearningService learningService;

    @Scheduled(cron = "0 0 0 * * *")
    public void executeAutomaticLearning() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        log.info("🤖 [QuestionPool 자동 학습 시작] 시각: {}", timestamp);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        try {
            long beforeSize = learningService.getQuestionPoolSize();
            double beforeAiRate = learningService.getAiUsageRate();
            
            log.info("📊 학습 전 상태:");
            log.info("   - QuestionPool 크기: {}개", beforeSize);
            log.info("   - AI 사용률: {:.2f}%", beforeAiRate);
            
            learningService.analyzeAndLearn();
            
            long afterSize = learningService.getQuestionPoolSize();
            double afterAiRate = learningService.getAiUsageRate();
            long newQuestions = afterSize - beforeSize;
            
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("✅ [QuestionPool 자동 학습 완료]");
            log.info("📈 학습 후 상태:");
            log.info("   - QuestionPool 크기: {}개 (+{}개)", afterSize, newQuestions);
            log.info("   - AI 사용률: {:.2f}% ({:.2f}%p 감소)", afterAiRate, beforeAiRate - afterAiRate);
            log.info("💰 예상 비용 절감:");
            log.info("   - 월간 질문 생성 1000개 기준");
            log.info("   - 절감액: ${:.2f}/월", (beforeAiRate - afterAiRate) * 10 * 0.15);
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            
        } catch (Exception e) {
            log.error("❌ QuestionPool 자동 학습 실패: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void generateLearningReport() {
        try {
            long poolSize = learningService.getQuestionPoolSize();
            double aiUsageRate = learningService.getAiUsageRate();
            double costSavings = (100 - aiUsageRate) * 10 * 0.15;
            
            log.info("📋 [일일 학습 리포트]");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("📦 QuestionPool 현황:");
            log.info("   - 총 질문 수: {}개", poolSize);
            log.info("   - 시스템 성숙도: {}", getMaturityLevel(poolSize));
            log.info("");
            log.info("🤖 AI 효율성:");
            log.info("   - AI 호출 비율: {:.2f}%", aiUsageRate);
            log.info("   - 캐시 히트율: {:.2f}%", 100 - aiUsageRate);
            log.info("");
            log.info("💰 비용 절감:");
            log.info("   - 월간 절감액: ${:.2f}", costSavings);
            log.info("   - 연간 절감액: ${:.2f}", costSavings * 12);
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            
        } catch (Exception e) {
            log.error("❌ 학습 리포트 생성 실패: {}", e.getMessage(), e);
        }
    }

    private String getMaturityLevel(long poolSize) {
        if (poolSize >= 5000) return "완전 성숙 🌟";
        if (poolSize >= 1000) return "고도화 단계 🚀";
        if (poolSize >= 500) return "성장 중 📈";
        if (poolSize >= 100) return "초기 단계 🌱";
        return "데이터 수집 중 🔍";
    }
}
