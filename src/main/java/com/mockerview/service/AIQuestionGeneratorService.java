package com.mockerview.service;

import com.mockerview.entity.Category;
import com.mockerview.entity.Question;
import com.mockerview.entity.Session;
import com.mockerview.repository.QuestionRepository;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIQuestionGeneratorService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final QuestionRepository questionRepository;

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openaiApiUrl;

    private static final Map<String, String> CATEGORY_PROMPTS = Map.of(
        "IT_DEV", "백엔드, 프론트엔드, 데이터베이스, 알고리즘 등 개발 관련",
        "MARKETING", "SNS 마케팅, 브랜딩, 광고 전략, 시장 분석 등",
        "DESIGN", "UI/UX, 그래픽 디자인, 사용자 경험, 디자인 프로세스 등",
        "SALES", "영업 전략, 고객 관리, 협상 기술, 세일즈 프로세스 등",
        "HR", "인사 관리, 채용, 조직 문화, 갈등 관리 등",
        "FINANCE", "재무 분석, 회계, 투자, 리스크 관리 등",
        "PUBLIC", "공무원 면접, 공공기관, 정책, 행정 등",
        "MEDICAL", "간호, 의료, 환자 관리, 의료 윤리 등"
    );

    private static final Map<Integer, String> DIFFICULTY_DESCRIPTIONS = Map.of(
        1, "초급: 기본 개념, 용어 정의, 간단한 경험 질문",
        2, "초중급: 실무 기초, 간단한 상황 대처, 기본 프로세스",
        3, "중급: 실무 경험, 문제 해결, 프로젝트 사례",
        4, "중고급: 복잡한 상황 대처, 전략적 사고, 깊이 있는 분석",
        5, "고급: 고난도 기술, 리더십, 혁신적 솔루션, 전문가 수준"
    );

    private static final Map<String, String> QUESTION_TYPES = Map.of(
        "TECHNICAL", "기술/전문 지식 질문",
        "BEHAVIORAL", "경험 기반 행동 질문 (STAR 기법)",
        "SITUATIONAL", "가상 상황 대처 질문",
        "PERSONALITY", "인성/가치관 질문"
    );

    public Question generateQuestion(Category category, Integer difficulty, String questionType, Session session, String previousAnswerFeedback) {
        try {
            String promptHash = generatePromptHash(category.getCode(), difficulty, questionType);
            
            Optional<Question> cached = questionRepository.findByAiPromptHash(promptHash);
            if (cached.isPresent() && new Random().nextDouble() > 0.3) {
                log.info("✅ 캐시된 질문 재사용: {}", cached.get().getText());
                return cached.get();
            }

            String prompt = buildPrompt(category, difficulty, questionType, previousAnswerFeedback);
            String generatedText = callOpenAI(prompt);

            Question question = Question.builder()
                    .text(generatedText)
                    .category(category)
                    .difficultyLevel(difficulty)
                    .questionType(questionType)
                    .isAiGenerated(true)
                    .aiPromptHash(promptHash)
                    .session(session)
                    .orderNo(session.getQuestions().size() + 1)
                    .timer(120)
                    .build();

            Question saved = questionRepository.save(question);
            log.info("✨ 새 질문 생성 및 캐싱: {}", saved.getText());
            return saved;

        } catch (Exception e) {
            log.error("AI 질문 생성 실패, 기본 질문 반환", e);
            return createFallbackQuestion(category, difficulty, questionType, session);
        }
    }

    public List<String> generateMultipleQuestions(Category category, Integer difficulty, String questionType, Integer count) {
        List<String> questions = new ArrayList<>();
        
        try {
            String prompt = buildMultipleQuestionsPrompt(category, difficulty, questionType, count);
            String response = callOpenAI(prompt);
            
            String[] lines = response.split("\n");
            for (String line : lines) {
                String cleaned = line.trim().replaceAll("^\\d+\\.\\s*", "").replaceAll("^-\\s*", "");
                if (!cleaned.isEmpty() && cleaned.length() > 10) {
                    questions.add(cleaned);
                }
            }
            
            if (questions.size() < count) {
                for (int i = questions.size(); i < count; i++) {
                    questions.add(createFallbackQuestionText(category, difficulty, i + 1));
                }
            }
            
            log.info("✨ {}개 질문 생성 완료", questions.size());
            
        } catch (Exception e) {
            log.error("AI 복수 질문 생성 실패, 기본 질문 반환", e);
            for (int i = 0; i < count; i++) {
                questions.add(createFallbackQuestionText(category, difficulty, i + 1));
            }
        }
        
        return questions.subList(0, Math.min(count, questions.size()));
    }

    private String buildMultipleQuestionsPrompt(Category category, Integer difficulty, String questionType, Integer count) {
        String categoryContext = category.getDescription() != null ? category.getDescription() : category.getName();
        String difficultyDesc = DIFFICULTY_DESCRIPTIONS.getOrDefault(difficulty, "중급");
        String typeDesc = QUESTION_TYPES.getOrDefault(questionType, "기술 질문");

        return String.format(
            "당신은 전문 면접관입니다. 다음 조건에 맞는 면접 질문을 정확히 %d개 생성하세요.\n\n" +
            "직무 분야: %s\n" +
            "난이도: %s\n" +
            "질문 유형: %s\n\n" +
            "요구사항:\n" +
            "1. 각 질문을 새로운 줄에 작성\n" +
            "2. 번호나 기호 없이 질문만 작성\n" +
            "3. 명확하고 구체적인 질문\n" +
            "4. 실제 면접에서 나올 법한 현실적인 질문\n" +
            "5. 각 질문은 100자 이내로 간결하게\n" +
            "6. 중복되지 않는 다양한 질문\n\n" +
            "질문만 작성하세요:",
            count, categoryContext, difficultyDesc, typeDesc
        );
    }

    private String createFallbackQuestionText(Category category, Integer difficulty, int number) {
        String[] templates = {
            "%s 분야에서 가장 자신있는 기술이나 경험은 무엇인가요?",
            "%s 업무를 수행하면서 가장 어려웠던 점과 해결 방법을 설명해주세요.",
            "%s 관련 프로젝트 경험이 있다면 구체적으로 말씀해주세요.",
            "%s 직무에 지원한 이유와 본인의 강점을 설명해주세요.",
            "%s 분야에서 최근 관심있게 본 트렌드나 기술이 있나요?"
        };
        
        int index = (number - 1) % templates.length;
        return String.format(templates[index], category.getName());
    }

    private String buildPrompt(Category category, Integer difficulty, String questionType, String previousFeedback) {
        String categoryContext = CATEGORY_PROMPTS.getOrDefault(category.getCode(), category.getName());
        String difficultyDesc = DIFFICULTY_DESCRIPTIONS.getOrDefault(difficulty, "중급");
        String typeDesc = QUESTION_TYPES.getOrDefault(questionType, "기술 질문");

        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 전문 면접관입니다. 다음 조건에 맞는 면접 질문을 1개만 생성하세요.\n\n");
        prompt.append("직무 분야: ").append(categoryContext).append("\n");
        prompt.append("난이도: ").append(difficultyDesc).append("\n");
        prompt.append("질문 유형: ").append(typeDesc).append("\n\n");

        if (previousFeedback != null && !previousFeedback.isEmpty()) {
            prompt.append("이전 답변 피드백: ").append(previousFeedback).append("\n");
            prompt.append("→ 이를 고려하여 다음 질문을 생성하세요.\n\n");
        }

        prompt.append("요구사항:\n");
        prompt.append("1. 질문만 출력하세요 (설명이나 부가 텍스트 없이)\n");
        prompt.append("2. 명확하고 구체적인 질문\n");
        prompt.append("3. 실제 면접에서 나올 법한 현실적인 질문\n");
        prompt.append("4. 100자 이내로 간결하게\n");

        return prompt.toString();
    }

    private String callOpenAI(String prompt) throws Exception {
        if (openaiApiKey == null || openaiApiKey.equals("${OPENAI_API_KEY}") || openaiApiKey.isEmpty()) {
            throw new IllegalStateException("OpenAI API Key not configured");
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o-mini");
        requestBody.put("messages", Arrays.asList(
            Map.of("role", "system", "content", "당신은 전문 면접관입니다. 질문만 간결하게 생성하세요."),
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", 0.8);
        requestBody.put("max_tokens", 500);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + openaiApiKey);
        headers.set("Content-Type", "application/json");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(openaiApiUrl, request, String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        return root.path("choices").get(0).path("message").path("content").asText().trim();
    }

    private Question createFallbackQuestion(Category category, Integer difficulty, String questionType, Session session) {
        String fallbackText = String.format("%s 분야에서 가장 자신있는 기술이나 경험은 무엇인가요?", category.getName());
        
        return Question.builder()
                .text(fallbackText)
                .category(category)
                .difficultyLevel(difficulty)
                .questionType(questionType)
                .isAiGenerated(false)
                .session(session)
                .orderNo(session.getQuestions().size() + 1)
                .timer(120)
                .build();
    }

    private String generatePromptHash(String categoryCode, Integer difficulty, String questionType) {
        try {
            String input = categoryCode + "-" + difficulty + "-" + questionType + "-" + System.currentTimeMillis() / 1000;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    public List<String> getSupportedCategories() {
        return new ArrayList<>(CATEGORY_PROMPTS.keySet());
    }

    public Map<String, String> getCategoryDescriptions() {
        return new HashMap<>(CATEGORY_PROMPTS);
    }
}
