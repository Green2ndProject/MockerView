package com.mockerview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockerview.entity.Category;
import com.mockerview.entity.Question;
import com.mockerview.entity.QuestionPool;
import com.mockerview.entity.Session;
import com.mockerview.repository.QuestionPoolRepository;
import com.mockerview.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedQuestionGeneratorService {

    private final QuestionPoolRepository questionPoolRepository;
    private final QuestionRepository questionRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openaiApiUrl;

    private static final List<String> DEFAULT_QUESTIONS = Arrays.asList(
        "자기소개를 해주세요.",
        "지원 동기가 무엇인가요?",
        "본인의 강점과 약점은 무엇인가요?",
        "5년 후 자신의 모습은 어떨 것 같나요?",
        "최근에 관심 있는 기술이나 트렌드는 무엇인가요?",
        "팀 프로젝트에서 어려움을 겪었던 경험이 있나요?",
        "본인만의 문제 해결 방식을 설명해주세요.",
        "실패한 경험과 그로부터 배운 점은 무엇인가요?",
        "이 직무를 선택한 특별한 이유가 있나요?",
        "최근에 가장 어려웠던 과제는 무엇이었나요?"
    );

    public Question generateQuestion(Category category, Integer difficultyLevel, String questionType, Session session) {
        log.info("🎯 질문 생성 시작 - 카테고리: {}, 난이도: {}, 타입: {}", 
                category.getName(), difficultyLevel, questionType);
        
        Question question = tryGenerateWithAI(category, difficultyLevel, questionType, session);
        if (question != null) {
            log.info("✅ AI 질문 생성 성공: {}", question.getText());
            return question;
        }
        
        question = tryGenerateFromPool(category, difficultyLevel, questionType, session);
        if (question != null) {
            log.info("✅ QuestionPool 질문 사용: {}", question.getText());
            return question;
        }
        
        question = generateDefaultQuestion(category, difficultyLevel, questionType, session);
        log.info("✅ 기본 질문 사용: {}", question.getText());
        return question;
    }

    private Question tryGenerateWithAI(Category category, Integer difficultyLevel, String questionType, Session session) {
        if (!isAIAvailable()) {
            log.warn("⚠️ OpenAI API Key 없음 - AI 건너뜀");
            return null;
        }

        try {
            String prompt = buildPrompt(category, difficultyLevel, questionType);
            String questionText = callOpenAI(prompt);
            
            Question question = Question.builder()
                    .text(questionText)
                    .category(category)
                    .difficultyLevel(difficultyLevel)
                    .questionType(questionType)
                    .session(session)
                    .isAiGenerated(true)
                    .timer(120)
                    .build();
            
            return questionRepository.save(question);
            
        } catch (Exception e) {
            log.error("❌ AI 질문 생성 실패: {}", e.getMessage());
            return null;
        }
    }

    private Question tryGenerateFromPool(Category category, Integer difficultyLevel, String questionType, Session session) {
        try {
            List<QuestionPool> poolQuestions = questionPoolRepository.findAll();
            
            if (poolQuestions.isEmpty()) {
                log.warn("⚠️ QuestionPool 비어있음");
                return null;
            }
            
            List<QuestionPool> filtered = poolQuestions.stream()
                    .filter(q -> q.getCategory() != null && q.getCategory().contains(category.getCode()))
                    .collect(Collectors.toList());
            
            if (filtered.isEmpty()) {
                filtered = poolQuestions;
            }
            
            Collections.shuffle(filtered);
            QuestionPool poolQuestion = filtered.get(0);
            
            Question question = Question.builder()
                    .text(poolQuestion.getText())
                    .category(category)
                    .difficultyLevel(difficultyLevel)
                    .questionType(questionType)
                    .session(session)
                    .isAiGenerated(false)
                    .timer(120)
                    .build();
            
            return questionRepository.save(question);
            
        } catch (Exception e) {
            log.error("❌ QuestionPool 조회 실패: {}", e.getMessage());
            return null;
        }
    }

    private Question generateDefaultQuestion(Category category, Integer difficultyLevel, String questionType, Session session) {
        int existingCount = session.getQuestions() != null ? session.getQuestions().size() : 0;
        String questionText = DEFAULT_QUESTIONS.get(existingCount % DEFAULT_QUESTIONS.size());
        
        Question question = Question.builder()
                .text(questionText)
                .category(category)
                .difficultyLevel(difficultyLevel)
                .questionType(questionType)
                .session(session)
                .isAiGenerated(false)
                .timer(120)
                .build();
        
        return questionRepository.save(question);
    }

    private boolean isAIAvailable() {
        return openaiApiKey != null 
                && !openaiApiKey.isEmpty() 
                && !openaiApiKey.equals("${OPENAI_API_KEY}");
    }

    private String buildPrompt(Category category, Integer difficultyLevel, String questionType) {
        String difficultyDesc = getDifficultyDescription(difficultyLevel);
        String typeDesc = getQuestionTypeDescription(questionType);
        
        return String.format(
            "당신은 전문 면접관입니다. 다음 조건에 맞는 면접 질문을 1개만 생성하세요.\n\n" +
            "직무 분야: %s (%s)\n" +
            "난이도: %s\n" +
            "질문 유형: %s\n\n" +
            "요구사항:\n" +
            "1. 질문만 출력하세요 (설명이나 부가 텍스트 없이)\n" +
            "2. 명확하고 구체적인 질문\n" +
            "3. 실제 면접에서 나올 법한 현실적인 질문\n" +
            "4. 100자 이내로 간결하게",
            category.getName(), 
            category.getDescription() != null ? category.getDescription() : "",
            difficultyDesc, 
            typeDesc
        );
    }

    private String callOpenAI(String prompt) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o-mini");
        requestBody.put("messages", Arrays.asList(
            Map.of("role", "system", "content", "당신은 전문 면접관입니다. 질문만 간결하게 생성하세요."),
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", 0.8);
        requestBody.put("max_tokens", 200);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + openaiApiKey);
        headers.set("Content-Type", "application/json");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        log.info("🔄 OpenAI 호출 중...");
        ResponseEntity<String> response = restTemplate.postForEntity(openaiApiUrl, request, String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        String content = root.path("choices").get(0).path("message").path("content").asText().trim();
        
        if (content.isEmpty()) {
            throw new IllegalStateException("AI 응답이 비어있음");
        }
        
        return content;
    }

    private String getDifficultyDescription(Integer level) {
        return switch (level) {
            case 1 -> "초급: 기본 개념, 용어 정의, 간단한 경험 질문";
            case 2 -> "초중급: 실무 기초, 간단한 상황 대처, 기본 프로세스";
            case 3 -> "중급: 실무 경험, 문제 해결, 프로젝트 사례";
            case 4 -> "중고급: 복잡한 상황 대처, 전략적 사고, 깊이 있는 분석";
            case 5 -> "고급: 고난도 기술, 리더십, 혁신적 솔루션, 전문가 수준";
            default -> "중급";
        };
    }

    private String getQuestionTypeDescription(String type) {
        return switch (type) {
            case "TECHNICAL" -> "기술/전문 지식 질문";
            case "BEHAVIORAL" -> "경험 기반 행동 질문 (STAR 기법)";
            case "SITUATIONAL" -> "가상 상황 대처 질문";
            case "PERSONALITY" -> "인성/가치관 질문";
            default -> "기술 질문";
        };
    }
}
