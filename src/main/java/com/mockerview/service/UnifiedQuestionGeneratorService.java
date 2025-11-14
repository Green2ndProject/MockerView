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
    private final QuestionPoolLearningService questionPoolLearningService;
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
        long poolSize = questionPoolLearningService.getQuestionPoolSize();
        double aiUsageRate = questionPoolLearningService.getAiUsageRate();
        
        log.info("🎯 질문 생성 시작 - 카테고리: {}, 난이도: {}, Pool 크기: {}개, AI 사용률: {}%", 
                category.getName(), difficultyLevel, poolSize, String.format("%.1f", aiUsageRate));
        
        Question question = tryGenerateFromPool(category, difficultyLevel, questionType, session);
        if (question != null) {
            log.info("✅ QuestionPool 질문 사용 (비용 절감!) - Pool 크기: {}개", poolSize);
            return question;
        }
        
        question = tryGenerateWithAI(category, difficultyLevel, questionType, session);
        if (question != null) {
            log.info("✅ AI 질문 생성 성공 (비용 발생) - AI 사용률: {}%", String.format("%.1f", aiUsageRate));
            return question;
        }
        
        question = generateDefaultQuestion(category, difficultyLevel, questionType, session);
        log.info("✅ 기본 질문 사용: {}", question.getText());
        return question;
    }

    private Question tryGenerateFromPool(Category category, Integer difficultyLevel, String questionType, Session session) {
        try {
            List<QuestionPool> poolQuestions = questionPoolRepository.findAll();
            
            if (poolQuestions.isEmpty()) {
                log.warn("⚠️ QuestionPool 비어있음 - AI 생성 필요");
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
            
            question = questionRepository.save(question);
            
            log.info("🧠 AI 생성 질문 → 자동 학습 대기열 추가: {}", question.getId());
            
            return question;
            
        } catch (Exception e) {
            log.error("❌ AI 질문 생성 실패: {}", e.getMessage());
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
        return openaiApiKey != null && !openaiApiKey.trim().isEmpty();
    }

    private String buildPrompt(Category category, Integer difficultyLevel, String questionType) {
        String difficultyDesc = switch (difficultyLevel) {
            case 1 -> "초급 (기본 개념 위주)";
            case 2 -> "초중급 (실무 기초)";
            case 3 -> "중급 (실무 경험 필요)";
            case 4 -> "중고급 (전략적 사고)";
            case 5 -> "고급 (전문가 수준)";
            default -> "중급";
        };

        return String.format("""
            면접 질문을 1개만 생성해주세요.
            
            - 카테고리: %s
            - 난이도: %s
            - 질문 타입: %s
            - STAR 기법으로 답변 가능한 행동 기반 질문
            - 번호나 특수문자 없이 질문만 작성
            
            형식: 질문 하나만 반환
            """, category.getName(), difficultyDesc, questionType);
    }

    private String callOpenAI(String prompt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + openaiApiKey);
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = Map.of(
            "model", "gpt-4o-mini",
            "messages", List.of(
                Map.of("role", "system", "content", "당신은 전문 면접관입니다."),
                Map.of("role", "user", "content", prompt)
            ),
            "temperature", 0.7,
            "max_tokens", 200
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(openaiApiUrl, request, String.class);

        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        return jsonNode.path("choices").get(0).path("message").path("content").asText().trim();
    }
}
