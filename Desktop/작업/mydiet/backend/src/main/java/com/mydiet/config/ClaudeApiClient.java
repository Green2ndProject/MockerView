package com.mydiet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class ClaudeApiClient {

    @Value("${claude.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String askClaude(String prompt) {
        System.out.println("=== Claude API 호출 시작 ===");
        System.out.println("API Key: " + (apiKey != null ? "설정됨" : "없음"));
        System.out.println("Prompt: " + prompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> requestBody = Map.of(
            "model", "claude-3-haiku-20240307",
            "max_tokens", 1000,
            "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            System.out.println("=== API 요청 전송 중 ===");
            ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.anthropic.com/v1/messages",
                entity,
                Map.class
            );

            System.out.println("=== API 응답 받음 ===");
            System.out.println("Response: " + response.getBody());

            if (response.getBody() != null && response.getBody().get("content") != null) {
                List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
                if (!content.isEmpty()) {
                    return (String) content.get(0).get("text");
                }
            }
            
            return "Claude가 응답하지 않았습니다.";
        } catch (Exception e) {
            System.err.println("=== Claude API 오류 ===");
            System.err.println("오류 메시지: " + e.getMessage());
            e.printStackTrace();
            return "잠깐, 다이어트에 집중하자! 💪 (AI 일시 오류)";
        }
    }
}