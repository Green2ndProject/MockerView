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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SelfInterviewService {

    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionPoolRepository questionPoolRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${openai.api.key}")
    private String openaiApiKey;
    
    @Value("${openai.api.url}")
    private String openaiApiUrl;

    @Transactional
    public Session createSelfInterviewSession(
            User user,
            String title,
            Integer questionCount,
            String sessionType,
            String difficulty,
            String category) {
        
        Session session = Session.builder()
            .host(user)
            .title(title)
            .sessionStatus(Session.SessionStatus.RUNNING)
            .sessionType(sessionType != null ? sessionType : "TEXT")
            .isSelfInterview("Y")
            .isReviewable("Y")
            .difficulty(difficulty)
            .category(category)
            .createdAt(LocalDateTime.now())
            .startTime(LocalDateTime.now())
            .lastActivity(LocalDateTime.now())
            .build();

        Session savedSession = sessionRepository.save(session);

        List<String> aiQuestions = generateQuestionsWithAI(difficulty, category, questionCount);
        
        int orderNo = 1;
        for (String questionText : aiQuestions) {
            Question question = Question.builder()
                .session(savedSession)
                .text(questionText)
                .orderNo(orderNo++)
                .questioner(user)
                .timer(120)
                .build();
            questionRepository.save(question);
        }

        log.info("AI 셀프 면접 생성 완료 - sessionId: {}, type: {}, difficulty: {}, category: {}, 질문 수: {}", 
            savedSession.getId(), sessionType, difficulty, category, aiQuestions.size());
        
        return savedSession;
    }

    private List<String> generateQuestionsWithAI(String difficulty, String category, Integer count) {
        try {
            String difficultyKr = getDifficultyKorean(difficulty);
            String categoryKr = getCategoryKorean(category);
            
            String prompt = String.format(
                "당신은 면접 질문 생성 전문가입니다. " +
                "난이도: %s, 카테고리: %s에 맞는 면접 질문 %d개를 생성해주세요.\n\n" +
                "규칙:\n" +
                "1. 각 질문은 한 줄로 작성\n" +
                "2. 번호 없이 질문만 작성\n" +
                "3. 난이도와 카테고리에 적합한 질문\n" +
                "4. 실제 면접에서 나올 법한 질문\n" +
                "5. 각 질문은 물음표(?)로 끝나야 함\n\n" +
                "예시:\n" +
                "자기소개를 해주세요.\n" +
                "가장 기억에 남는 프로젝트는 무엇인가요?\n\n" +
                "질문:",
                difficultyKr, categoryKr, count
            );

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o-mini");
            requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "당신은 전문 면접관입니다. 항상 한국어로 질문을 생성합니다."),
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("max_tokens", 1000);
            requestBody.put("temperature", 0.7);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + openaiApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("AI 질문 생성 요청 - 난이도: {}, 카테고리: {}, 개수: {}", difficulty, category, count);

            ResponseEntity<Map> response = restTemplate.postForEntity(openaiApiUrl, entity, Map.class);

            Map<String, Object> responseBody = response.getBody();
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            log.info("AI 응답 수신: {}", content.substring(0, Math.min(100, content.length())));

            List<String> questions = parseQuestions(content, count);
            
            if (questions.isEmpty()) {
                log.warn("AI 질문 파싱 실패, Fallback 사용");
                return getFallbackQuestions(difficulty, category, count);
            }
            
            log.info("AI 질문 생성 성공: {}개", questions.size());
            return questions;
            
        } catch (Exception e) {
            log.error("AI 질문 생성 실패, QuestionPool 사용: ", e);
            return getFallbackQuestions(difficulty, category, count);
        }
    }

    private List<String> parseQuestions(String content, Integer count) {
        String[] lines = content.split("\n");
        List<String> questions = new ArrayList<>();
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && 
                !trimmed.startsWith("#") && 
                !trimmed.startsWith("규칙") &&
                !trimmed.startsWith("예시") &&
                (trimmed.contains("?") || trimmed.contains("요") || trimmed.contains("나요") || trimmed.contains("까"))) {
                
                trimmed = trimmed.replaceFirst("^\\d+\\.\\s*", "");
                trimmed = trimmed.replaceFirst("^\\d+\\)\\s*", "");
                trimmed = trimmed.replaceFirst("^-\\s*", "");
                trimmed = trimmed.replaceFirst("^\\*\\s*", "");
                
                if (trimmed.length() > 5) {
                    questions.add(trimmed);
                }
            }
            
            if (questions.size() >= count) {
                break;
            }
        }
        
        while (questions.size() < count) {
            questions.add("자기소개를 해주세요.");
        }
        
        return questions.subList(0, Math.min(count, questions.size()));
    }

    private List<String> getFallbackQuestions(String difficulty, String category, Integer count) {
        log.info("Fallback: QuestionPool에서 질문 가져오기");
        
        List<QuestionPool> poolQuestions = getFilteredQuestions(difficulty, category, count);
        
        if (poolQuestions.isEmpty()) {
            log.warn("QuestionPool에도 질문이 없음, 기본 질문 사용");
            return getDefaultQuestions(count);
        }
        
        return poolQuestions.stream()
            .map(QuestionPool::getText)
            .collect(Collectors.toList());
    }

    private List<String> getDefaultQuestions(Integer count) {
        List<String> defaults = List.of(
            "자기소개를 해주세요.",
            "지원 동기는 무엇인가요?",
            "본인의 강점과 약점을 말씀해주세요.",
            "가장 기억에 남는 프로젝트를 소개해주세요.",
            "팀에서 갈등이 생겼을 때 어떻게 해결하나요?",
            "5년 후 본인의 모습은 어떨 것 같나요?",
            "실패했던 경험과 그로부터 배운 점을 말씀해주세요.",
            "왜 우리 회사에 지원하셨나요?",
            "본인만의 특별한 경험이 있다면 말씀해주세요.",
            "마지막으로 하고 싶은 말씀이 있나요?"
        );
        
        return defaults.subList(0, Math.min(count, defaults.size()));
    }

    private List<QuestionPool> getFilteredQuestions(String difficulty, String category, Integer count) {
        boolean hasDifficulty = difficulty != null && !difficulty.isEmpty() && !"ALL".equals(difficulty);
        boolean hasCategory = category != null && !category.isEmpty() && !"ALL".equals(category);
        
        if (hasDifficulty && hasCategory) {
            log.info("난이도({})와 카테고리({})로 질문 검색", difficulty, category);
            return questionPoolRepository.findRandomQuestionsByDifficultyAndCategory(
                difficulty, category, count);
        } else if (hasDifficulty) {
            log.info("난이도({})로 질문 검색", difficulty);
            return questionPoolRepository.findRandomQuestionsByDifficulty(difficulty, count);
        } else if (hasCategory) {
            log.info("카테고리({})로 질문 검색", category);
            return questionPoolRepository.findRandomQuestionsByCategory(category, count);
        } else {
            log.info("전체 질문에서 랜덤 검색");
            return questionPoolRepository.findRandomQuestions(count);
        }
    }

    @Transactional
    public Session createSelfInterview(
            Long userId, 
            String interviewType, 
            Integer questionCount,
            String difficulty,
            String category) {
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        return createSelfInterviewSession(
            user,
            "셀프 면접 - " + interviewType,
            questionCount,
            "TEXT",
            difficulty,
            category
        );
    }

    @Transactional
    public Session createSelfInterviewWithAI(
            Long userId, 
            String interviewType, 
            Integer questionCount,
            String difficulty,
            String category) {
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        return createSelfInterviewSession(
            user,
            "AI 셀프 면접 - " + interviewType,
            questionCount,
            "TEXT",
            difficulty,
            category
        );
    }

    private String getDifficultyKorean(String difficulty) {
        if (difficulty == null) return "보통";
        switch(difficulty) {
            case "EASY": return "쉬움";
            case "HARD": return "어려움";
            case "MEDIUM": return "보통";
            default: return "보통";
        }
    }

    private String getCategoryKorean(String category) {
        if (category == null) return "일반 질문";
        switch(category) {
            case "TECHNICAL": return "기술면접";
            case "PERSONALITY": return "인성면접";
            case "PROJECT": return "프로젝트 경험";
            case "SITUATION": return "상황 대처";
            case "GENERAL": return "일반 질문";
            default: return "일반 질문";
        }
    }
}
