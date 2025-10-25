package com.mockerview.controller.api;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.Answer;
import com.mockerview.entity.Feedback;
import com.mockerview.entity.Question;
import com.mockerview.entity.User;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.FeedbackRepository;
import com.mockerview.repository.QuestionRepository;
import com.mockerview.repository.UserRepository;
import com.mockerview.service.OpenAIService;
import com.mockerview.service.FacialAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/selfinterview")
@RequiredArgsConstructor
@Slf4j
public class SelfInterviewAnswerController {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final OpenAIService openAIService;
    private final FacialAnalysisService facialAnalysisService;

    @PostMapping(value = "/{sessionId}/answer", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> submitAnswer(
            @PathVariable Long sessionId,
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            Long questionId = Long.valueOf(payload.get("questionId").toString());
            String answerText = payload.get("answerText").toString();
            
            log.info("답변 제출 - sessionId: {}, questionId: {}, userId: {}", 
                    sessionId, questionId, userDetails.getUserId());
            
            Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
            
            User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Answer answer = Answer.builder()
                .question(question)
                .user(user)
                .answerText(answerText)
                .aiFeedbackRequested(true)
                .createdAt(LocalDateTime.now())
                .build();
            
            answerRepository.save(answer);
            
            String aiAnalysis = openAIService.analyzeAnswer(question.getText(), answerText);
            
            answer.setAiFeedbackGenerated(true);
            answerRepository.save(answer);
            
            Feedback feedback = Feedback.builder()
                .answer(answer)
                .reviewer(null)
                .feedbackType(Feedback.FeedbackType.AI)
                .score(extractScore(aiAnalysis))
                .summary("AI 자동 피드백")
                .strengths(extractStrengths(aiAnalysis))
                .weaknesses(extractWeaknesses(aiAnalysis))
                .improvementSuggestions(extractImprovements(aiAnalysis))
                .createdAt(LocalDateTime.now())
                .build();
            
            feedbackRepository.save(feedback);
            
            facialAnalysisService.analyzeFaceAsync(answer.getId(), null);
            log.info("🎥 표정 분석 비동기 호출 완료 - answerId: {}", answer.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("answer", Map.of("id", answer.getId(), "text", answerText));
            response.put("feedback", Map.of(
                "score", feedback.getScore(),
                "strengths", feedback.getStrengths(),
                "improvements", feedback.getImprovementSuggestions()
            ));
            
            log.info("✅ 답변 제출 완료 - answerId: {}, 점수: {}", answer.getId(), feedback.getScore());
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
            
        } catch (Exception e) {
            log.error("답변 제출 실패", e);
            return ResponseEntity.internalServerError()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    private int extractScore(String aiAnalysis) {
        try {
            if (aiAnalysis.contains("점수:") || aiAnalysis.contains("Score:")) {
                String[] parts = aiAnalysis.split("점수:|Score:");
                String scorePart = parts[1].split("\n")[0].trim();
                return Integer.parseInt(scorePart.replaceAll("[^0-9]", ""));
            }
        } catch (Exception e) {
            log.warn("점수 추출 실패, 기본값 75점 반환");
        }
        return 75;
    }
    
    private String extractStrengths(String aiAnalysis) {
        try {
            if (aiAnalysis.contains("강점:") || aiAnalysis.contains("Strengths:")) {
                String[] parts = aiAnalysis.split("강점:|Strengths:");
                return parts[1].split("약점:|개선점:|Weaknesses:|Improvements:")[0].trim();
            }
        } catch (Exception e) {
            log.warn("강점 추출 실패");
        }
        return "답변을 성실히 작성하셨습니다.";
    }
    
    private String extractWeaknesses(String aiAnalysis) {
        try {
            if (aiAnalysis.contains("약점:") || aiAnalysis.contains("Weaknesses:")) {
                String[] parts = aiAnalysis.split("약점:|Weaknesses:");
                return parts[1].split("개선점:|Improvements:")[0].trim();
            }
        } catch (Exception e) {
            log.warn("약점 추출 실패");
        }
        return "";
    }
    
    private String extractImprovements(String aiAnalysis) {
        try {
            if (aiAnalysis.contains("개선점:") || aiAnalysis.contains("Improvements:")) {
                String[] parts = aiAnalysis.split("개선점:|Improvements:");
                return parts[1].trim();
            }
        } catch (Exception e) {
            log.warn("개선점 추출 실패");
        }
        return "더 구체적인 예시를 추가해보세요.";
    }
}