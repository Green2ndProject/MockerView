package com.mydiet.mydiet.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ClaudeApiClient {

    @Value("${claude.api.key}")
    private String apiKey;
    
    @Value("${claude.api.base-url}")
    private String baseUrl;
    
    @Value("${claude.api.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    public String askClaude(String prompt) {
        return askClaude(prompt, 0.8, 300);
    }
    
    public String askClaude(String prompt, double temperature, int maxTokens) {
        try {
            HttpHeaders headers = createHeaders();
            Map<String, Object> requestBody = createRequestBody(prompt, temperature, maxTokens);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            log.info("Claude API 요청 시작 - 모델: {}", model);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/messages",
                entity,
                Map.class
            );
            
            String claudeResponse = extractResponse(response);
            log.info("Claude API 응답 성공 - 길이: {}", claudeResponse.length());
            
            return claudeResponse;
            
        } catch (HttpClientErrorException e) {
            log.error("Claude API 클라이언트 오류 - 상태: {}, 메시지: {}", 
                     e.getStatusCode(), e.getResponseBodyAsString());
            return handleClientError(e);
            
        } catch (HttpServerErrorException e) {
            log.error("Claude API 서버 오류 - 상태: {}, 메시지: {}", 
                     e.getStatusCode(), e.getResponseBodyAsString());
            return "Claude 서버에 일시적인 문제가 있습니다. 잠시 후 다시 시도해주세요.";
            
        } catch (Exception e) {
            log.error("Claude API 호출 중 예상치 못한 오류", e);
            return "Claude와 연결하는 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요.";
        }
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");
        return headers;
    }
    
    private Map<String, Object> createRequestBody(String prompt, double temperature, int maxTokens) {
        return Map.of(
            "model", model,
            "max_tokens", maxTokens,
            "temperature", temperature,
            "messages", List.of(Map.of(
                "role", "user",
                "content", prompt
            ))
        );
    }
    
    private String extractResponse(ResponseEntity<Map> response) {
        try {
            Map<String, Object> body = response.getBody();
            if (body == null) {
                return "Claude로부터 빈 응답을 받았습니다.";
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) body.get("content");
            
            if (content == null || content.isEmpty()) {
                return "Claude 응답 형식이 올바르지 않습니다.";
            }
            
            String text = (String) content.get(0).get("text");
            return text != null ? text.trim() : "Claude로부터 텍스트 응답을 받지 못했습니다.";
            
        } catch (Exception e) {
            log.error("Claude 응답 파싱 중 오류", e);
            return "Claude 응답을 처리하는 중 오류가 발생했습니다.";
        }
    }
    
    private String handleClientError(HttpClientErrorException e) {
        return switch (e.getStatusCode().value()) {
            case 400 -> "잘못된 요청입니다. 관리자에게 문의하세요.";
            case 401 -> "Claude API 인증에 실패했습니다. API 키를 확인해주세요.";
            case 403 -> "Claude API 접근 권한이 없습니다.";
            case 404 -> "요청한 모델을 찾을 수 없습니다. 모델명을 확인해주세요.";
            case 429 -> "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.";
            default -> "Claude API 요청 중 오류가 발생했습니다: " + e.getMessage();
        };
    }
}
