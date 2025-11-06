package com.mockerview.controller.api;

import com.mockerview.entity.Answer;
import com.mockerview.entity.FacialAnalysis;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.FacialAnalysisRepository;
import com.mockerview.service.FacialAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug/facial")
@RequiredArgsConstructor
@Slf4j
public class FacialAnalysisDebugController {

    private final FacialAnalysisService facialAnalysisService;
    private final FacialAnalysisRepository facialAnalysisRepository;
    private final AnswerRepository answerRepository;

    @PostMapping("/analyze/{answerId}")
    public ResponseEntity<Map<String, Object>> forceAnalyze(@PathVariable Long answerId) {
        try {
            log.info("🧪 강제 표정 분석 시작 - answerId: {}", answerId);
            
            Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Answer not found"));
            
            facialAnalysisService.analyzeFaceAsync(answerId, null);
            
            Thread.sleep(3000);
            
            List<FacialAnalysis> analyses = facialAnalysisRepository
                .findByAnswerUserIdOrderByCreatedAtDesc(answer.getUser().getId());
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "표정 분석 완료");
            result.put("totalAnalyses", analyses.size());
            
            if (!analyses.isEmpty()) {
                FacialAnalysis latest = analyses.get(0);
                result.put("latestAnalysis", Map.of(
                    "id", latest.getId(),
                    "smileScore", latest.getSmileScore(),
                    "eyeContactScore", latest.getEyeContactScore(),
                    "confidenceScore", latest.getConfidenceScore()
                ));
            }
            
            log.info("✅ 강제 표정 분석 완료 - count: {}", analyses.size());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ 강제 표정 분석 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/check/{userId}")
    public ResponseEntity<Map<String, Object>> checkAnalyses(@PathVariable Long userId) {
        try {
            log.info("🔍 표정 분석 조회 - userId: {}", userId);
            
            List<FacialAnalysis> analyses = facialAnalysisRepository
                .findByAnswerUserIdOrderByCreatedAtDesc(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("totalCount", analyses.size());
            result.put("hasAnalyses", !analyses.isEmpty());
            
            if (!analyses.isEmpty()) {
                result.put("latestScore", analyses.get(0).getConfidenceScore());
                result.put("latestCreatedAt", analyses.get(0).getCreatedAt());
            }
            
            log.info("✅ 표정 분석 조회 완료 - count: {}", analyses.size());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ 표정 분석 조회 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}