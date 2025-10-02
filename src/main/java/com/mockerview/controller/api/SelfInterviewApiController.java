package com.mockerview.controller.api;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.*;
import com.mockerview.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/selfinterview")
@RequiredArgsConstructor
@Slf4j
public class SelfInterviewApiController {

    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${openai.api.key}")
    private String openaiApiKey;
    
    @Value("${openai.api.url}")
    private String openaiApiUrl;

    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSessionData(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Session session = sessionRepository.findById(sessionId).orElse(null);
        
        if (session == null || !session.getHost().getId().equals(userDetails.getUserId())) {
            log.warn("Session not found or unauthorized - sessionId: {}, userId: {}", sessionId, userDetails.getUserId());
            return ResponseEntity.notFound().build();
        }

        List<Question> questions = questionRepository.findBySessionIdOrderByOrderNo(sessionId);
        
        List<Map<String, Object>> questionMaps = questions.stream().map(q -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", q.getId());
            map.put("questionText", q.getText());
            map.put("orderNumber", q.getOrderNo());
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getId());
        response.put("userId", userDetails.getUserId());
        response.put("title", session.getTitle());
        response.put("questions", questionMaps);

        log.info("Session data sent - sessionId: {}, questions: {}", sessionId, questionMaps.size());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{sessionId}/answer")
    public ResponseEntity<Map<String, Object>> submitAnswer(
            @PathVariable Long sessionId,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            Long questionId = Long.valueOf(request.get("questionId").toString());
            String answerText = request.get("answerText").toString();

            log.info("Answer submitted - sessionId: {}, questionId: {}, userId: {}", 
                     sessionId, questionId, userDetails.getUserId());

            Question question = questionRepository.findById(questionId).orElseThrow();
            User user = userRepository.findById(userDetails.getUserId()).orElseThrow();

            Answer answer = Answer.builder()
                    .question(question)
                    .user(user)
                    .answerText(answerText)
                    .build();
            answer = answerRepository.save(answer);

            String feedbackText = generateAIFeedback(question.getText(), answerText);
            
            int score = extractScore(feedbackText);
            String strengths = extractSection(feedbackText, "강점");
            String weaknesses = extractSection(feedbackText, "개선점");

            Feedback feedback = new Feedback();
            feedback.setAnswer(answer);
            feedback.setScore(score);
            feedback.setStrengths(strengths);
            feedback.setWeaknesses(weaknesses);
            feedbackRepository.save(feedback);

            Map<String, Object> result = new HashMap<>();
            result.put("answer", Map.of("id", answer.getId(), "answerText", answerText));
            result.put("feedback", Map.of(
                "score", score,
                "strengths", strengths,
                "improvements", weaknesses
            ));

            log.info("Feedback generated - score: {}", score);

            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Failed to submit answer", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String generateAIFeedback(String questionText, String answerText) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o-mini");
            requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "당신은 면접 평가 전문가입니다. 답변을 평가하고 피드백을 제공하세요."),
                Map.of("role", "user", "content", String.format("질문: %s\n\n답변: %s\n\n위 답변을 평가하고 다음 형식으로 피드백을 제공해주세요:\n점수: (0-100)\n강점: (구체적인 강점)\n개선점: (구체적인 개선점)", questionText, answerText))
            ));
            requestBody.put("max_tokens", 500);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + openaiApiKey);
            headers.set("Content-Type", "application/json");

            org.springframework.http.HttpEntity<Map<String, Object>> entity = 
                new org.springframework.http.HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                openaiApiUrl, entity, Map.class);

            Map<String, Object> responseBody = response.getBody();
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            
            return (String) message.get("content");
        } catch (Exception e) {
            log.error("AI feedback generation failed", e);
            return "점수: 75\n강점: 질문에 대한 답변을 제공했습니다.\n개선점: 더 구체적인 예시와 함께 답변하면 좋습니다.";
        }
    }

    private int extractScore(String feedback) {
        try {
            if (feedback.contains("점수:")) {
                String scorePart = feedback.substring(feedback.indexOf("점수:") + 3).trim();
                String scoreStr = scorePart.split("[^0-9]")[0];
                return Integer.parseInt(scoreStr);
            }
        } catch (Exception e) {
            log.warn("Failed to extract score", e);
        }
        return 75;
    }

    private String extractSection(String feedback, String section) {
        try {
            if (feedback.contains(section + ":")) {
                String[] parts = feedback.split(section + ":");
                if (parts.length > 1) {
                    String content = parts[1].trim();
                    int nextSection = content.indexOf("\n\n");
                    if (nextSection > 0) {
                        content = content.substring(0, nextSection);
                    }
                    return content.trim();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract section: " + section, e);
        }
        return "분석 중...";
    }
}
