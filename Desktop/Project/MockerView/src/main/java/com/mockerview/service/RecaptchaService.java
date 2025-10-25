package com.mockerview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class RecaptchaService {

    @Value("${recaptcha.secret-key:}")
    private String secretKey;

    @Value("${recaptcha.verify-url:https://www.google.com/recaptcha/api/siteverify}")
    private String verifyUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean verify(String token) {
        if (secretKey == null || secretKey.isEmpty()) {
            log.warn("reCAPTCHA secret key not configured, skipping verification");
            return true;
        }

        if (token == null || token.isEmpty()) {
            log.warn("reCAPTCHA token is empty");
            return false;
        }

        try {
            String url = verifyUrl + "?secret=" + secretKey + "&response=" + token;
            
            String response = restTemplate.postForObject(url, null, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);

            boolean success = jsonNode.get("success").asBoolean();
            double score = jsonNode.has("score") ? jsonNode.get("score").asDouble() : 0.0;

            log.info("reCAPTCHA verification - success: {}, score: {}", success, score);

            return success && score >= 0.5;

        } catch (Exception e) {
            log.error("reCAPTCHA verification failed", e);
            return false;
        }
    }
}
