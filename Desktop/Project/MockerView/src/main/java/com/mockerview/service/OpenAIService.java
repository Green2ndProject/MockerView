package com.mockerview.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${openai.api.url}")
    private String openaiApiUrl;

    public String analyzeAnswer(String question, String answer) {
        if (openaiApiKey == null || openaiApiKey.equals("${OPENAI_API_KEY}") || openaiApiKey.isEmpty()) {
            log.warn("⚠️ OpenAI API Key가 설정되지 않음, 기본 피드백 반환");
            return "점수: 75\n강점: 답변을 성실히 작성하셨습니다.\n개선점: 더 구체적인 예시를 추가해보세요.";
        }

        try {
            String prompt = String.format(
                "다음 면접 질문에 대한 답변을 평가해주세요.\n\n" +
                "질문: %s\n" +
                "답변: %s\n\n" +
                "아래 형식으로 평가해주세요:\n" +
                "점수: [0-100점]\n" +
                "강점: [구체적인 강점 2-3가지]\n" +
                "개선점: [구체적인 개선점 2-3가지]",
                question, answer
            );
            
            return callOpenAI(prompt);
        } catch (Exception e) {
            log.error("AI 답변 분석 실패", e);
            return "점수: 75\n강점: 답변을 성실히 작성하셨습니다.\n개선점: 더 구체적인 예시를 들어보세요.";
        }
    }

    private String callOpenAI(String prompt) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-3.5-turbo");
        requestBody.put("messages", Arrays.asList(
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 500);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + openaiApiKey);
        headers.set("Content-Type", "application/json");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(openaiApiUrl, request, String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        return root.path("choices").get(0).path("message").path("content").asText();
    }
}
