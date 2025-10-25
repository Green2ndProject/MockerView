package com.mockerview.controller.api;

import com.mockerview.entity.FacialAnalysis;
import com.mockerview.entity.InterviewMBTI;
import com.mockerview.entity.VoiceAnalysis;
import com.mockerview.repository.FacialAnalysisRepository;
import com.mockerview.repository.InterviewMBTIRepository;
import com.mockerview.repository.UserRepository;
import com.mockerview.repository.VoiceAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Slf4j
public class AdvancedAnalysisController {

    private final VoiceAnalysisRepository voiceAnalysisRepository;
    private final FacialAnalysisRepository facialAnalysisRepository;
    private final InterviewMBTIRepository mbtiRepository;
    private final UserRepository userRepository;
    private final com.mockerview.service.InterviewMBTIService mbtiService;

    @GetMapping("/voice/user/{userId}")
    public ResponseEntity<?> getVoiceAnalyses(@PathVariable Long userId) {
        try {
            log.info("🎤 음성 분석 조회 - userId: {}", userId);
            
            List<VoiceAnalysis> analyses = voiceAnalysisRepository.findByAnswerUserIdOrderByCreatedAtDesc(userId);
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (VoiceAnalysis v : analyses) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", v.getId());
                map.put("speakingSpeed", v.getSpeakingSpeed());
                map.put("fillerWordsCount", v.getFillerWordsCount());
                map.put("pauseCount", v.getPauseCount());
                map.put("avgPauseDuration", v.getAvgPauseDuration());
                map.put("voiceStability", v.getVoiceStability());
                map.put("pronunciationScore", v.getPronunciationScore());
                map.put("fillerWordsDetail", v.getFillerWordsDetail());
                map.put("improvementSuggestions", v.getImprovementSuggestions());
                map.put("createdAt", v.getCreatedAt() != null ? v.getCreatedAt().toString() : null);
                result.add(map);
            }
            
            log.info("✅ 음성 분석 조회 성공 - count: {}", result.size());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ 음성 분석 조회 실패 - userId: {}", userId, e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @GetMapping("/facial/user/{userId}")
    public ResponseEntity<?> getFacialAnalyses(@PathVariable Long userId) {
        try {
            log.info("😊 표정 분석 조회 - userId: {}", userId);
            
            List<FacialAnalysis> analyses = facialAnalysisRepository.findByAnswerUserIdOrderByCreatedAtDesc(userId);
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (FacialAnalysis f : analyses) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", f.getId());
                map.put("smileScore", f.getSmileScore());
                map.put("eyeContactScore", f.getEyeContactScore());
                map.put("postureScore", f.getPostureScore());
                map.put("confidenceScore", f.getConfidenceScore());
                map.put("tensionLevel", f.getTensionLevel());
                map.put("detailedAnalysis", f.getDetailedAnalysis());
                map.put("improvementSuggestions", f.getImprovementSuggestions());
                map.put("createdAt", f.getCreatedAt() != null ? f.getCreatedAt().toString() : null);
                result.add(map);
            }
            
            log.info("✅ 표정 분석 조회 성공 - count: {}", result.size());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ 표정 분석 조회 실패 - userId: {}", userId, e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @GetMapping("/mbti/{userId}")
    public ResponseEntity<?> getMBTI(@PathVariable Long userId) {
        try {
            log.info("🧠 MBTI 조회 - userId: {}", userId);
            
            Optional<InterviewMBTI> mbtiOpt = mbtiRepository.findTopByUserIdOrderByCreatedAtDesc(userId);
            
            if (mbtiOpt.isEmpty()) {
                log.info("ℹ️ MBTI 없음 - userId: {}", userId);
                return ResponseEntity.status(404).body(Map.of("message", "MBTI 분석 결과가 없습니다"));
            }
            
            InterviewMBTI mbti = mbtiOpt.get();
            
            Map<String, Object> result = new HashMap<>();
            result.put("id", mbti.getId());
            result.put("mbtiType", mbti.getMbtiType());
            result.put("analyticalScore", mbti.getAnalyticalScore());
            result.put("creativeScore", mbti.getCreativeScore());
            result.put("logicalScore", mbti.getLogicalScore());
            result.put("emotionalScore", mbti.getEmotionalScore());
            result.put("detailOrientedScore", mbti.getDetailOrientedScore());
            result.put("bigPictureScore", mbti.getBigPictureScore());
            result.put("decisiveScore", mbti.getDecisiveScore());
            result.put("flexibleScore", mbti.getFlexibleScore());
            result.put("strengthDescription", mbti.getStrengthDescription());
            result.put("weaknessDescription", mbti.getWeaknessDescription());
            result.put("careerRecommendation", mbti.getCareerRecommendation());
            result.put("createdAt", mbti.getCreatedAt() != null ? mbti.getCreatedAt().toString() : null);
            
            log.info("✅ MBTI 조회 성공 - type: {}", mbti.getMbtiType());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ MBTI 조회 실패 - userId: {}", userId, e);
            return ResponseEntity.status(500).body(Map.of("message", "MBTI 조회 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/mbti/{userId}")
    public ResponseEntity<?> analyzeMBTI(@PathVariable Long userId) {
        try {
            log.info("🧠 MBTI 분석 시작 - userId: {}", userId);
            
            InterviewMBTI mbti = mbtiService.analyzeMBTI(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("id", mbti.getId());
            result.put("mbtiType", mbti.getMbtiType());
            result.put("analyticalScore", mbti.getAnalyticalScore());
            result.put("creativeScore", mbti.getCreativeScore());
            result.put("logicalScore", mbti.getLogicalScore());
            result.put("emotionalScore", mbti.getEmotionalScore());
            result.put("detailOrientedScore", mbti.getDetailOrientedScore());
            result.put("bigPictureScore", mbti.getBigPictureScore());
            result.put("decisiveScore", mbti.getDecisiveScore());
            result.put("flexibleScore", mbti.getFlexibleScore());
            result.put("strengthDescription", mbti.getStrengthDescription());
            result.put("weaknessDescription", mbti.getWeaknessDescription());
            result.put("careerRecommendation", mbti.getCareerRecommendation());
            result.put("createdAt", mbti.getCreatedAt() != null ? mbti.getCreatedAt().toString() : null);
            
            log.info("✅ MBTI 분석 완료 - type: {}", mbti.getMbtiType());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ MBTI 분석 실패 - userId: {}", userId, e);
            return ResponseEntity.status(500).body(Map.of("message", "분석 실패: " + e.getMessage()));
        }
    }

    @GetMapping("/comprehensive/{userId}")
    public ResponseEntity<?> getComprehensiveAnalysis(@PathVariable Long userId) {
        try {
            log.info("📊 종합 분석 조회 - userId: {}", userId);
            
            List<Map<String, Object>> voiceList = new ArrayList<>();
            List<Map<String, Object>> facialList = new ArrayList<>();
            Map<String, Object> mbtiData = null;
            
            try {
                List<VoiceAnalysis> voiceAnalyses = voiceAnalysisRepository.findByAnswerUserIdOrderByCreatedAtDesc(userId);
                for (VoiceAnalysis v : voiceAnalyses) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", v.getId());
                    map.put("speakingSpeed", v.getSpeakingSpeed());
                    map.put("pronunciationScore", v.getPronunciationScore());
                    map.put("createdAt", v.getCreatedAt() != null ? v.getCreatedAt().toString() : null);
                    voiceList.add(map);
                }
            } catch (Exception e) {
                log.warn("음성 분석 조회 실패", e);
            }
            
            try {
                List<FacialAnalysis> facialAnalyses = facialAnalysisRepository.findByAnswerUserIdOrderByCreatedAtDesc(userId);
                for (FacialAnalysis f : facialAnalyses) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", f.getId());
                    map.put("smileScore", f.getSmileScore());
                    map.put("confidenceScore", f.getConfidenceScore());
                    map.put("createdAt", f.getCreatedAt() != null ? f.getCreatedAt().toString() : null);
                    facialList.add(map);
                }
            } catch (Exception e) {
                log.warn("표정 분석 조회 실패", e);
            }
            
            try {
                Optional<InterviewMBTI> mbtiOpt = mbtiRepository.findTopByUserIdOrderByCreatedAtDesc(userId);
                if (mbtiOpt.isPresent()) {
                    InterviewMBTI mbti = mbtiOpt.get();
                    mbtiData = new HashMap<>();
                    mbtiData.put("mbtiType", mbti.getMbtiType());
                    mbtiData.put("strengthDescription", mbti.getStrengthDescription());
                    mbtiData.put("createdAt", mbti.getCreatedAt() != null ? mbti.getCreatedAt().toString() : null);
                }
            } catch (Exception e) {
                log.warn("MBTI 조회 실패", e);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("voiceAnalyses", voiceList);
            result.put("facialAnalyses", facialList);
            result.put("mbti", mbtiData);
            
            log.info("✅ 종합 분석 완료 - voice: {}, facial: {}, mbti: {}", 
                voiceList.size(), facialList.size(), mbtiData != null);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ 종합 분석 실패 - userId: {}", userId, e);
            Map<String, Object> result = new HashMap<>();
            result.put("voiceAnalyses", Collections.emptyList());
            result.put("facialAnalyses", Collections.emptyList());
            result.put("mbti", null);
            return ResponseEntity.ok(result);
        }
    }
}