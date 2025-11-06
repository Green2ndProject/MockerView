package com.mockerview.service;

import com.mockerview.entity.Question;
import com.mockerview.entity.QuestionPool;
import com.mockerview.entity.Session;
import com.mockerview.entity.User;
import com.mockerview.repository.QuestionPoolRepository;
import com.mockerview.repository.QuestionRepository;
import com.mockerview.repository.SessionRepository;
import com.mockerview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SelfInterviewService {

    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final QuestionPoolRepository questionPoolRepository;
    private final SubscriptionService subscriptionService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${openai.api.url}")
    private String openaiApiUrl;

    @Transactional
    public Session createSelfInterviewSession(Long userId, String title, String type, 
                                                String difficulty, String category, 
                                                int questionCount) {
        
        log.info("🎤 셀프 면접 세션 생성 시작 - userId: {}, title: {}, type: {}, difficulty: {}, category: {}, count: {}", 
                userId, title, type, difficulty, category, questionCount);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        log.info("🔒 구독 한도 체크 및 카운트 증가 시작 - userId: {}", user.getId());
        boolean allowed = subscriptionService.canCreateSessionAndIncrement(user.getId());
        if (!allowed) {
            log.error("❌ 세션 생성 한도 초과 - userId: {}", user.getId());
            throw new RuntimeException("세션 생성 한도에 도달했습니다. 플랜을 업그레이드해주세요.");
        }
        log.info("✅ 카운트 증가 완료 - userId: {}", user.getId());

        Session session = Session.builder()
                .host(user)
                .title(title)
                .sessionType(type)
                .sessionStatus(Session.SessionStatus.PLANNED)
                .isSelfInterview("Y")
                .difficulty(difficulty)
                .category(category)
                .aiEnabled(true)
                .aiMode("DETAILED")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        Session savedSession = sessionRepository.save(session);
        
        log.info("✅ 셀프 면접 세션 저장 완료 - sessionId: {}", savedSession.getId());

        List<String> aiQuestions = generateQuestionsWithAI(difficulty, category, questionCount);
        
        log.info("🔍 AI가 생성한 질문 목록:");
        for (int i = 0; i < aiQuestions.size(); i++) {
            log.info("  Q{}: {}", i+1, aiQuestions.get(i));
        }

        int orderNo = 1;
        for (String questionText : aiQuestions) {
            Question question = Question.builder()
                    .session(savedSession)
                    .text(questionText)
                    .orderNo(orderNo++)
                    .questioner(user)
                    .build();
            
            Question saved = questionRepository.save(question);
            log.info("💾 질문 저장 완료 - ID: {}, OrderNo: {}, Text: {}", 
                    saved.getId(), saved.getOrderNo(), saved.getText());
        }

        log.info("✅ AI 셀프 면접 생성 완료 - sessionId: {}, type: {}, difficulty: {}, category: {}, 질문 수: {}",
                savedSession.getId(), type, difficulty, category, aiQuestions.size());

        return savedSession;
    }

    private List<String> generateQuestionsWithAI(String difficulty, String category, int count) {
        if (openaiApiKey == null || openaiApiKey.equals("${OPENAI_API_KEY}") || openaiApiKey.isEmpty()) {
            log.warn("⚠️ OpenAI API Key가 설정되지 않았습니다. QuestionPool 사용");
            return getQuestionsFromPool(difficulty, category, count);
        }

        try {
            log.info("🤖 AI 질문 생성 요청 - 난이도: {}, 카테고리: {}, 개수: {}", difficulty, category, count);

            String prompt = String.format(
                "당신은 전문 면접관입니다. %s 카테고리의 %s 난이도 면접 질문을 정확히 %d개 생성해주세요.\n\n" +
                "중요 요구사항:\n" +
                "1. 각 질문은 서로 완전히 다른 주제여야 합니다 (중복 금지!)\n" +
                "2. 질문은 구체적이고 실무 중심적이어야 합니다\n" +
                "3. 난이도에 맞는 깊이로 질문해주세요\n" +
                "4. JSON 배열 형식으로만 답변: [\"질문1\", \"질문2\", ...]\n" +
                "5. 설명이나 부가 텍스트 없이 JSON 배열만 반환\n\n" +
                "난이도별 가이드:\n" +
                "- 쉬움: 기본 개념, 경험 중심\n" +
                "- 보통: 실무 상황, 문제해결\n" +
                "- 어려움: 심화 기술, 아키텍처, 전략\n",
                category, difficulty, count
            );

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("messages", Arrays.asList(
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 1000);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + openaiApiKey);
            headers.set("Content-Type", "application/json");

            org.springframework.http.HttpEntity<Map<String, Object>> request = 
                new org.springframework.http.HttpEntity<>(requestBody, headers);

            org.springframework.http.ResponseEntity<String> response = 
                restTemplate.postForEntity(openaiApiUrl, request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();
            
            log.info("🤖 AI 응답 수신: {}", content.substring(0, Math.min(100, content.length())));

            List<String> questions = parseQuestionsFromAI(content);
            
            if (questions.isEmpty()) {
                log.warn("⚠️ AI 질문 파싱 실패, Fallback 사용");
                return getQuestionsFromPool(difficulty, category, count);
            }

            log.info("✅ AI 질문 생성 성공: {}개", questions.size());
            return questions;

        } catch (Exception e) {
            log.error("❌ AI 질문 생성 실패, QuestionPool 사용: ", e);
            return getQuestionsFromPool(difficulty, category, count);
        }
    }

    private List<String> parseQuestionsFromAI(String content) {
        try {
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

            if (content.startsWith("[") && content.endsWith("]")) {
                JsonNode jsonNode = objectMapper.readTree(content);
                List<String> questions = new ArrayList<>();
                for (JsonNode node : jsonNode) {
                    questions.add(node.asText());
                }
                return questions;
            }

            return Arrays.stream(content.split("\n"))
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .filter(line -> line.matches(".*\\?$"))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("질문 파싱 실패", e);
            return new ArrayList<>();
        }
    }

    private List<String> getQuestionsFromPool(String difficulty, String category, int count) {
        log.info("📚 Fallback: QuestionPool에서 질문 가져오기");
        
        List<QuestionPool> poolQuestions = questionPoolRepository.findAll();
        
        if (poolQuestions.isEmpty()) {
            log.warn("⚠️ QuestionPool에도 질문이 없음, 기본 질문 사용");
            return getDefaultQuestions(count);
        }

        List<String> questions = poolQuestions.stream()
                .map(QuestionPool::getText)
                .collect(Collectors.toList());

        Collections.shuffle(questions);
        
        return questions.stream()
                .limit(count)
                .collect(Collectors.toList());
    }

    private List<String> getDefaultQuestions(int count) {
        List<String> defaultQuestions = Arrays.asList(
            "자기소개를 해주세요.",
            "지원 동기가 무엇인가요?",
            "본인의 강점과 약점은 무엇인가요?",
            "5년 후 자신의 모습은 어떨 것 같나요?",
            "최근에 읽은 책이나 관심 있는 기술은 무엇인가요?"
        );
        
        return defaultQuestions.stream()
                .limit(count)
                .collect(Collectors.toList());
    }
}