package com.mockerview.controller.api;

import com.mockerview.entity.Answer;
import com.mockerview.entity.FacialAnalysis;
import com.mockerview.entity.InterviewMBTI;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.FacialAnalysisRepository;
import com.mockerview.repository.InterviewMBTIRepository;
import com.mockerview.service.FacialAnalysisService;
import com.mockerview.service.InterviewMBTIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
@Slf4j
public class BatchAnalysisController {

    private final AnswerRepository answerRepository;
    private final FacialAnalysisRepository facialAnalysisRepository;
    private final InterviewMBTIRepository mbtiRepository;
    private final FacialAnalysisService facialAnalysisService;
    private final InterviewMBTIService mbtiService;

    @PostMapping("/facial/{userId}")
    public ResponseEntity<Map<String, Object>> batchAnalyzeFacial(@PathVariable Long userId) {
        try {
            log.info("🔄 일괄 표정 분석 시작 - userId: {}", userId);
            
            List<Answer> answers = answerRepository.findByUserIdOrderByCreatedAtDesc(userId);
            
            int totalAnswers = answers.size();
            int analyzedCount = 0;
            int skippedCount = 0;
            
            for (Answer answer : answers) {
                boolean hasAnalysis = facialAnalysisRepository
                    .findByAnswerUserIdOrderByCreatedAtDesc(answer.getUser().getId())
                    .stream()
                    .anyMatch(fa -> fa.getAnswer().getId().equals(answer.getId()));
                
                if (!hasAnalysis) {
                    facialAnalysisService.analyzeFaceAsync(answer.getId(), null);
                    analyzedCount++;
                    log.info("⏳ 표정 분석 중 - answerId: {} ({}/{})", answer.getId(), analyzedCount, totalAnswers);
                    Thread.sleep(500);
                } else {
                    skippedCount++;
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("totalAnswers", totalAnswers);
            result.put("analyzed", analyzedCount);
            result.put("skipped", skippedCount);
            result.put("message", analyzedCount + "개 답변 표정 분석 완료! 3초 후 새로고침하세요.");
            
            log.info("✅ 일괄 표정 분석 완료 - 분석: {}, 건너뜀: {}", analyzedCount, skippedCount);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ 일괄 표정 분석 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/status/{userId}")
    public ResponseEntity<Map<String, Object>> checkBatchStatus(@PathVariable Long userId) {
        try {
            log.info("📊 표정 분석 상태 확인 - userId: {}", userId);
            
            List<Answer> answers = answerRepository.findByUserIdOrderByCreatedAtDesc(userId);
            log.info("답변 개수: {}", answers.size());
            
            List<FacialAnalysis> analyses = facialAnalysisRepository
                .findByAnswerUserIdOrderByCreatedAtDesc(userId);
            log.info("표정 분석 개수: {}", analyses.size());
            
            int totalAnswers = answers.size();
            int analyzedAnswers = (int) answers.stream()
                .filter(answer -> analyses.stream()
                    .anyMatch(fa -> fa.getAnswer().getId().equals(answer.getId())))
                .count();
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalAnswers", totalAnswers);
            result.put("analyzedAnswers", analyzedAnswers);
            result.put("missingCount", totalAnswers - analyzedAnswers);
            result.put("needsAnalysis", totalAnswers > analyzedAnswers);
            
            log.info("✅ 표정 분석 상태: total={}, analyzed={}, missing={}", 
                totalAnswers, analyzedAnswers, totalAnswers - analyzedAnswers);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ 표정 분석 상태 확인 실패 - userId: {}", userId, e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/mbti/{userId}")
    public ResponseEntity<Map<String, Object>> batchAnalyzeMBTI(@PathVariable Long userId) {
        try {
            log.info("🔄 MBTI 분석 시작 - userId: {}", userId);
            
            List<Answer> answers = answerRepository.findByUserIdOrderByCreatedAtDesc(userId);
            log.info("답변 개수: {}", answers.size());
            
            if (answers.size() < 5) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "MBTI 분석을 위해 최소 5개 이상의 답변이 필요합니다. (현재: " + answers.size() + "개)"
                ));
            }

            InterviewMBTI result = mbtiService.analyzeMBTI(userId);
            log.info("✅ MBTI 저장 완료 - id: {}, type: {}", result.getId(), result.getMbtiType());
            
            InterviewMBTI saved = mbtiRepository.findById(result.getId()).orElse(null);
            if (saved == null) {
                log.error("❌ MBTI 저장 후 조회 실패!");
                return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "MBTI 저장은 됐지만 조회 실패"
                ));
            }
            
            log.info("✅ MBTI 저장 확인 완료 - id: {}, type: {}", saved.getId(), saved.getMbtiType());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalAnswers", answers.size());
            response.put("mbtiType", result.getMbtiType());
            response.put("mbtiId", result.getId());
            response.put("message", "MBTI 분석 완료! 3초 후 새로고침하세요.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ MBTI 분석 실패 - userId: {}", userId, e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "분석 실패: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/mbti/status/{userId}")
    public ResponseEntity<Map<String, Object>> checkMBTIStatus(@PathVariable Long userId) {
        try {
            log.info("📊 MBTI 상태 확인 - userId: {}", userId);
            
            List<Answer> answers = answerRepository.findByUserIdOrderByCreatedAtDesc(userId);
            log.info("답변 개수: {}", answers.size());
            
            Optional<InterviewMBTI> mbtiOpt = mbtiRepository.findLatestByUserId(userId);
            log.info("MBTI 존재 여부: {}", mbtiOpt.isPresent());
            
            boolean hasMBTI = mbtiOpt.isPresent();
            boolean hasEnoughAnswers = answers.size() >= 5;
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalAnswers", answers.size());
            result.put("hasMBTI", hasMBTI);
            result.put("hasEnoughAnswers", hasEnoughAnswers);
            result.put("needsAnalysis", !hasMBTI && hasEnoughAnswers);
            result.put("minAnswers", 5);
            
            log.info("✅ MBTI 상태: total={}, hasMBTI={}, needsAnalysis={}", 
                answers.size(), hasMBTI, !hasMBTI && hasEnoughAnswers);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ MBTI 상태 확인 실패 - userId: {}", userId, e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            errorResult.put("totalAnswers", 0);
            errorResult.put("hasMBTI", false);
            errorResult.put("hasEnoughAnswers", false);
            errorResult.put("needsAnalysis", false);
            errorResult.put("minAnswers", 5);
            
            return ResponseEntity.status(500).body(errorResult);
        }
    }
}