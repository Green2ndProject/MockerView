package com.mockerview.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AIFeedbackController {

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeFeedback(@RequestBody Map<String, String> request) {
        try {
            String question = request.get("question");
            String answer = request.get("answer");
            
            log.info("AI 피드백 요청 - 질문: {}, 답변: {}", question, answer);
            
            if (answer == null || answer.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "답변이 비어있습니다."));
            }
            
            String prompt = String.format(
                "다음 면접 답변을 분석해주세요.\n\n질문: %s\n답변: %s\n\n" +
                "다음 형식으로 JSON을 반환해주세요:\n" +
                "{\n" +
                "  \"score\": 0-10점,\n" +
                "  \"strengths\": \"강점 설명\",\n" +
                "  \"weaknesses\": \"약점 설명\",\n" +
                "  \"improvements\": \"개선 방안\"\n" +
                "}",
                question != null ? question : "질문 없음",
                answer
            );
            
            RestTemplate restTemplate = new RestTemplate();
            String openaiUrl = "https://api.openai.com/v1/chat/completions";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o-mini");
            requestBody.put("messages", new Object[]{
                Map.of("role", "system", "content", "당신은 전문 면접관입니다. 답변을 분석하고 건설적인 피드백을 제공합니다."),
                Map.of("role", "user", "content", prompt)
            });
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 1000);
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + openaiApiKey);
            headers.set("Content-Type", "application/json");
            
            org.springframework.http.HttpEntity<Map<String, Object>> entity = 
                new org.springframework.http.HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(openaiUrl, entity, Map.class);
            
            if (response.getBody() != null && response.getBody().containsKey("choices")) {
                var choices = (java.util.List<?>) response.getBody().get("choices");
                if (!choices.isEmpty()) {
                    var choice = (Map<?, ?>) choices.get(0);
                    var message = (Map<?, ?>) choice.get("message");
                    String content = (String) message.get("content");
                    
                    log.info("OpenAI 응답: {}", content);
                    
                    content = content.trim();
                    if (content.startsWith("```json")) {
                        content = content.substring(7);
                    }
                    if (content.startsWith("```")) {
                        content = content.substring(3);
                    }
                    if (content.endsWith("```")) {
                        content = content.substring(0, content.length() - 3);
                    }
                    content = content.trim();
                    
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, Object> feedback = mapper.readValue(content, Map.class);
                    
                    log.info("AI 피드백 생성 완료");
                    return ResponseEntity.ok(feedback);
                }
            }
            
            log.warn("OpenAI 응답이 비어있음");
            return ResponseEntity.ok(Map.of(
                "score", 5,
                "strengths", "답변을 제공해주셨습니다.",
                "weaknesses", "더 구체적인 내용이 필요합니다.",
                "improvements", "경험을 바탕으로 답변해보세요."
            ));
            
        } catch (Exception e) {
            log.error("AI 피드백 생성 실패", e);
            return ResponseEntity.ok(Map.of(
                "score", 5,
                "strengths", "답변 감사합니다.",
                "weaknesses", "AI 분석 중 오류가 발생했습니다.",
                "improvements", "다시 시도해주세요."
            ));
        }
    }
}
