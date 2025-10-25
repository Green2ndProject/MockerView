package com.mockerview.controller.api;

import com.mockerview.entity.Answer;
import com.mockerview.entity.InterviewMBTI;
import com.mockerview.entity.User;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.InterviewMBTIRepository;
import com.mockerview.repository.UserRepository;
import com.mockerview.service.InterviewMBTIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug/mbti")
@RequiredArgsConstructor
@Slf4j
public class MBTIDebugController {

    private final InterviewMBTIService mbtiService;
    private final InterviewMBTIRepository mbtiRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;

    @PostMapping("/analyze/{userId}")
    public ResponseEntity<Map<String, Object>> forceAnalyze(@PathVariable Long userId) {
        try {
            log.info("🧪 강제 MBTI 분석 시작 - userId: {}", userId);
            
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            List<Answer> answers = answerRepository.findByUserIdOrderByCreatedAtDesc(userId);
            
            if (answers.size() < 5) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "MBTI 분석을 위해 최소 5개 이상의 답변이 필요합니다.",
                    "currentAnswers", answers.size(),
                    "requiredAnswers", 5
                ));
            }
            
            InterviewMBTI mbti = mbtiService.analyzeMBTI(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "MBTI 분석 완료");
            result.put("mbtiType", mbti.getMbtiType());
            result.put("analyticalScore", mbti.getAnalyticalScore());
            result.put("creativeScore", mbti.getCreativeScore());
            result.put("strengthDescription", mbti.getStrengthDescription());
            result.put("careerRecommendation", mbti.getCareerRecommendation());
            
            log.info("✅ 강제 MBTI 분석 완료 - type: {}", mbti.getMbtiType());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ 강제 MBTI 분석 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/check/{userId}")
    public ResponseEntity<Map<String, Object>> checkMBTI(@PathVariable Long userId) {
        try {
            log.info("🔍 MBTI 조회 - userId: {}", userId);
            
            List<Answer> answers = answerRepository.findByUserIdOrderByCreatedAtDesc(userId);
            InterviewMBTI mbti = mbtiRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElse(null);
            
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("totalAnswers", answers.size());
            result.put("hasMBTI", mbti != null);
            result.put("canAnalyze", answers.size() >= 5);
            
            if (mbti != null) {
                result.put("mbtiType", mbti.getMbtiType());
                result.put("createdAt", mbti.getCreatedAt());
            }
            
            log.info("✅ MBTI 조회 완료 - hasMBTI: {}, answers: {}", mbti != null, answers.size());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ MBTI 조회 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}