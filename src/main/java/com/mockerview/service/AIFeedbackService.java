package com.mockerview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockerview.dto.FeedbackMessage;
import com.mockerview.entity.Answer;
import com.mockerview.entity.Feedback;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIFeedbackService {
    
    @Value("${openai.api.key}")
    private String apiKey;
    
    @Value("${openai.api.url}")
    private String apiUrl;
    
    private final AnswerRepository answerRepository;
    private final FeedbackRepository feedbackRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Async
    @Transactional
    public void generateFeedbackAsync(Long answerId, Long sessionId) {
        log.info("Starting AI feedback generation for answer: {}, session: {}", answerId, sessionId);
        
        try {
            Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Answer not found: " + answerId));
            
            Hibernate.initialize(answer.getQuestion());
            Hibernate.initialize(answer.getUser());
            
            log.info("Answer found: {}", answer.getText().substring(0, Math.min(50, answer.getText().length())));
            
            String prompt = createFeedbackPrompt(answer);
            log.info("Calling OpenAI API...");
            
            String aiResponse = callOpenAI(prompt);
            log.info("OpenAI response received: {}", aiResponse.substring(0, Math.min(100, aiResponse.length())));
            
            Feedback feedback = parseFeedbackResponse(aiResponse, answer);
            feedback = feedbackRepository.save(feedback);
            log.info("Feedback saved with ID: {}", feedback.getId());
            
            FeedbackMessage feedbackMessage = FeedbackMessage.builder()
                .sessionId(sessionId)
                .answerId(answerId)
                .userId(answer.getUser().getId())
                .summary(feedback.getSummary())
                .strengths(feedback.getStrengths())
                .weaknesses(feedback.getWeaknesses())
                .improvement(feedback.getImprovement())
                .model(feedback.getModel())
                .timestamp(LocalDateTime.now())
                .build();
            
            String destination = "/topic/session/" + sessionId + "/feedback";
            log.info("Sending feedback to WebSocket destination: {}", destination);
            
            messagingTemplate.convertAndSend(destination, feedbackMessage);
            log.info("AI feedback generation completed successfully");
            
        } catch (Exception e) {
            log.error("Error generating AI feedback for answer {}: ", answerId, e);
            sendErrorFeedback(answerId, sessionId, e.getMessage());
        }
    }

    private void sendErrorFeedback(Long answerId, Long sessionId, String errorMsg) {
        try {
            Answer answer = answerRepository.findById(answerId).orElse(null);
            if (answer == null) return;
            
            Feedback errorFeedback = Feedback.builder()
                .answer(answer)
                .summary("피드백 생성 중 오류가 발생했습니다.")
                .strengths("답변을 제출해주셔서 감사합니다.")
                .weaknesses("AI 서비스가 일시적으로 불안정합니다.")
                .improvement("잠시 후 다시 시도해주세요. 오류: " + errorMsg)
                .model("ERROR")
                .build();
            
            feedbackRepository.save(errorFeedback);
            
            FeedbackMessage errorFeedbackMessage = FeedbackMessage.builder()
                .sessionId(sessionId)
                .answerId(answerId)
                .userId(answer.getUser().getId())
                .summary(errorFeedback.getSummary())
                .strengths(errorFeedback.getStrengths())
                .weaknesses(errorFeedback.getWeaknesses())
                .improvement(errorFeedback.getImprovement())
                .model(errorFeedback.getModel())
                .timestamp(LocalDateTime.now())
                .build();
            
            messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/feedback", errorFeedbackMessage);
            
        } catch (Exception e) {
            log.error("Failed to send error feedback: ", e);
        }
    }

    private String createFeedbackPrompt(Answer answer) {
        String question = answer.getQuestion().getText();
        String answerText = answer.getText();
        
        return String.format("""
            면접 질문과 답변을 분석하여 피드백을 제공해주세요.
            
            질문: %s
            답변: %s
            
            다음 형식으로 JSON 응답해주세요:
            {
                "summary": "답변에 대한 간단한 요약 (50자 이내)",
                "strengths": "답변의 강점들 (구체적으로 2-3가지)",
                "weaknesses": "답변의 약점들 (건설적으로 2-3가지)",
                "improvement": "개선 방안 제안 (실행 가능한 조언)"
            }
            
            각 항목은 구체적이고 건설적으로 작성해주세요.
            """, question, answerText);
    }

    private String callOpenAI(String prompt) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RuntimeException("OpenAI API key not configured");
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.replace("Bearer ", ""));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o-mini");
        requestBody.put("messages", List.of(
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("max_tokens", 800);
        requestBody.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            log.info("Sending request to OpenAI: {}", apiUrl);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);
            
            if (response.getBody() == null) {
                throw new RuntimeException("Empty response from OpenAI");
            }
            
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            
            if (jsonNode.has("error")) {
                String errorMessage = jsonNode.path("error").path("message").asText();
                throw new RuntimeException("OpenAI API Error: " + errorMessage);
            }
            
            String content = jsonNode.path("choices").get(0).path("message").path("content").asText();
            if (content.isEmpty()) {
                throw new RuntimeException("Empty content from OpenAI response");
            }
            
            return content;
            
        } catch (Exception e) {
            log.error("Error calling OpenAI API: ", e);
            throw new RuntimeException("AI 피드백 생성 실패: " + e.getMessage(), e);
        }
    }

    private Feedback parseFeedbackResponse(String aiResponse, Answer answer) {
        try {
            String cleanResponse = aiResponse.trim();
            if (cleanResponse.startsWith("```json")) {
                cleanResponse = cleanResponse.substring(7);
            }
            if (cleanResponse.endsWith("```")) {
                cleanResponse = cleanResponse.substring(0, cleanResponse.length() - 3);
            }
            
            JsonNode feedback = objectMapper.readTree(cleanResponse);
            
            return Feedback.builder()
                .answer(answer)
                .summary(feedback.path("summary").asText("답변이 제출되었습니다."))
                .strengths(feedback.path("strengths").asText("답변해주셔서 감사합니다."))
                .weaknesses(feedback.path("weaknesses").asText("추가 개선이 필요합니다."))
                .improvement(feedback.path("improvement").asText("더 구체적인 설명을 추가해보세요."))
                .model("GPT-4O-MINI")
                .build();
                
        } catch (Exception e) {
            log.error("Error parsing AI response: {}", aiResponse, e);
            
            return Feedback.builder()
                .answer(answer)
                .summary("답변이 제출되었습니다.")
                .strengths("답변해주셔서 감사합니다.")
                .weaknesses("AI 응답 파싱 중 오류가 발생했습니다.")
                .improvement("기술적인 문제로 상세 피드백을 제공할 수 없습니다.")
                .model("GPT-4O-MINI")
                .build();
        }
    }
}