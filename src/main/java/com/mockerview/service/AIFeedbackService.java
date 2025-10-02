package com.mockerview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockerview.dto.FeedbackMessage;
import com.mockerview.entity.Answer;
import com.mockerview.entity.Feedback;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIFeedbackService {
    
    @Value("${openai.api.key}")
    private String apiKey;
    
    @Value("${openai.api.url}")
    private String apiUrl;
    
    private final AnswerRepository answerRepository;
    private final FeedbackRepository feedbackRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Async
    @Transactional
    public void generateFeedbackAsync(Long answerId, Long sessionId) {
        log.info("Starting AI feedback generation for answer: {}, session: {}", answerId, sessionId);
        
        try {
            Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Answer not found: " + answerId));
            
            Hibernate.initialize(answer.getQuestion());
            Hibernate.initialize(answer.getUser());
            
            log.info("Answer found: {}", answer.getAnswerText().substring(0, Math.min(50, answer.getAnswerText().length())));
            
            String prompt = createFeedbackPrompt(answer);
            log.info("Calling OpenAI API...");
            
            String aiResponse = callOpenAI(prompt);
            log.info("OpenAI response received: {}", aiResponse.substring(0, Math.min(100, aiResponse.length())));
            
            Feedback feedback = parseFeedbackResponse(aiResponse, answer);
            feedback = feedbackRepository.save(feedback);
            log.info("Feedback saved with ID: {}", feedback.getId());
            
            FeedbackMessage feedbackMessage = FeedbackMessage.builder()
                .sessionId(sessionId)
                .answerId(answerId)
                .userId(answer.getUser().getId())
                .summary(feedback.getSummary())
                .strengths(feedback.getStrengths())
                .weaknesses(feedback.getWeaknesses())
                .improvement(feedback.getImprovement())
                .model(feedback.getModel())
                .timestamp(LocalDateTime.now())
                .build();
            
            String destination = "/topic/session/" + sessionId + "/feedback";
            log.info("Sending feedback to WebSocket destination: {}", destination);
            
            messagingTemplate.convertAndSend(destination, feedbackMessage);
            log.info("AI feedback generation completed successfully");
            
        } catch (Exception e) {
            log.error("Error generating AI feedback for answer {}: ", answerId, e);
            sendErrorFeedback(answerId, sessionId, e.getMessage());
        }
    }

    public List<String> generateInterviewQuestions(int count) {
        log.info("Generating {} interview questions", count);
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("OpenAI API key not configured, returning dummy questions");
            return generateDummyQuestions(count);
        }

        try {
            String prompt = String.format("""
                면접 질문 %d개를 생성해주세요. 각 질문은 한 줄로 작성하고, 번호나 특수문자 없이 질문만 작성해주세요.
                
                다음 카테고리를 골고루 포함해주세요:
                - 자기소개 및 경력
                - 기술 역량
                - 문제 해결 능력
                - 협업 및 커뮤니케이션
                - 성장 마인드
                
                각 질문은 구체적이고 답변하기에 적절한 수준이어야 합니다.
                """, count);

            String aiResponse = callOpenAI(prompt);
            List<String> questions = parseQuestions(aiResponse);
            
            while (questions.size() < count) {
                questions.add("추가 질문 " + (questions.size() + 1) + ": 본인의 강점과 약점에 대해 말씀해주세요.");
            }

            return questions.subList(0, Math.min(count, questions.size()));
            
        } catch (Exception e) {
            log.error("Error generating interview questions: ", e);
            return generateDummyQuestions(count);
        }
    }

    public Map<String, Object> generateFeedbackSync(String question, String answer) {
        log.info("Generating synchronous feedback for answer");
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("OpenAI API key not configured, returning dummy feedback");
            return generateDummyFeedback(answer);
        }

        try {
            String prompt = String.format("""
                다음은 면접 질문과 지원자의 답변입니다.
                
                질문: %s
                
                답변: %s
                
                다음 루브릭 기준으로 평가해주세요:
                1. 내용의 완성도 (30점): 질문에 대한 직접적이고 명확한 답변
                2. 구체성 (25점): 구체적인 사례와 경험 제시
                3. 논리성 (25점): 답변의 구조와 흐름
                4. 전문성 (20점): 해당 분야에 대한 이해도
                
                다음 형식으로 JSON 응답해주세요:
                {
                    "score": 85,
                    "strengths": "강점에 대한 구체적인 설명 (2-3문장)",
                    "improvements": "개선이 필요한 부분과 구체적인 조언 (2-3문장)"
                }
                """, question, answer);

            String aiResponse = callOpenAI(prompt);
            return parseFeedbackJson(aiResponse);
            
        } catch (Exception e) {
            log.error("Error generating synchronous feedback: ", e);
            return generateDummyFeedback(answer);
        }
    }

    private void sendErrorFeedback(Long answerId, Long sessionId, String errorMsg) {
        try {
            Answer answer = answerRepository.findById(answerId).orElse(null);
            if (answer == null) return;
            
            Feedback errorFeedback = Feedback.builder()
                .answer(answer)
                .summary("피드백 생성 중 오류가 발생했습니다.")
                .strengths("답변을 제출해주셔서 감사합니다.")
                .weaknesses("AI 서비스가 일시적으로 불안정합니다.")
                .improvement("잠시 후 다시 시도해주세요. 오류: " + errorMsg)
                .model("ERROR")
                .build();
            
            feedbackRepository.save(errorFeedback);
            
            FeedbackMessage errorFeedbackMessage = FeedbackMessage.builder()
                .sessionId(sessionId)
                .answerId(answerId)
                .userId(answer.getUser().getId())
                .summary(errorFeedback.getSummary())
                .strengths(errorFeedback.getStrengths())
                .weaknesses(errorFeedback.getWeaknesses())
                .improvement(errorFeedback.getImprovement())
                .model(errorFeedback.getModel())
                .timestamp(LocalDateTime.now())
                .build();
            
            messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/feedback", errorFeedbackMessage);
            
        } catch (Exception e) {
            log.error("Failed to send error feedback: ", e);
        }
    }

    private String createFeedbackPrompt(Answer answer) {
        String question = answer.getQuestion().getText();
        String answerText = answer.getAnswerText();
        
        return String.format("""
            면접 질문과 답변을 분석하여 피드백을 제공해주세요.
            
            질문: %s
            답변: %s
            
            다음 형식으로 JSON 응답해주세요:
            {
                "summary": "답변에 대한 간단한 요약 (50자 이내)",
                "strengths": "답변의 강점들 (구체적으로 2-3가지)",
                "weaknesses": "답변의 약점들 (건설적으로 2-3가지)",
                "improvement": "개선 방안 제안 (실행 가능한 조언)"
            }
            
            각 항목은 구체적이고 건설적으로 작성해주세요.
            """, question, answerText);
    }

    private String callOpenAI(String prompt) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RuntimeException("OpenAI API key not configured");
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.replace("Bearer ", ""));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o-mini");
        requestBody.put("messages", List.of(
            Map.of("role", "system", "content", "당신은 전문 면접관이자 커리어 코치입니다. 루브릭 기반으로 객관적이고 건설적인 피드백을 제공합니다."),
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("max_tokens", 800);
        requestBody.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            log.info("Sending request to OpenAI: {}", apiUrl);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);
            
            if (response.getBody() == null) {
                throw new RuntimeException("Empty response from OpenAI");
            }
            
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            
            if (jsonNode.has("error")) {
                String errorMessage = jsonNode.path("error").path("message").asText();
                throw new RuntimeException("OpenAI API Error: " + errorMessage);
            }
            
            String content = jsonNode.path("choices").get(0).path("message").path("content").asText();
            if (content.isEmpty()) {
                throw new RuntimeException("Empty content from OpenAI response");
            }
            
            return content;
            
        } catch (Exception e) {
            log.error("Error calling OpenAI API: ", e);
            throw new RuntimeException("AI 피드백 생성 실패: " + e.getMessage(), e);
        }
    }

    private Feedback parseFeedbackResponse(String aiResponse, Answer answer) {
        try {
            String cleanResponse = aiResponse.trim();
            if (cleanResponse.startsWith("```json")) {
                cleanResponse = cleanResponse.substring(7);
            }
            if (cleanResponse.endsWith("```")) {
                cleanResponse = cleanResponse.substring(0, cleanResponse.length() - 3);
            }
            
            JsonNode feedback = objectMapper.readTree(cleanResponse);
            
            return Feedback.builder()
                .answer(answer)
                .summary(feedback.path("summary").asText("답변이 제출되었습니다."))
                .strengths(feedback.path("strengths").asText("답변해주셔서 감사합니다."))
                .weaknesses(feedback.path("weaknesses").asText("추가 개선이 필요합니다."))
                .improvement(feedback.path("improvement").asText("더 구체적인 설명을 추가해보세요."))
                .model("GPT-4O-MINI")
                .build();
                
        } catch (Exception e) {
            log.error("Error parsing AI response: {}", aiResponse, e);
            
            return Feedback.builder()
                .answer(answer)
                .summary("답변이 제출되었습니다.")
                .strengths("답변해주셔서 감사합니다.")
                .weaknesses("AI 응답 파싱 중 오류가 발생했습니다.")
                .improvement("기술적인 문제로 상세 피드백을 제공할 수 없습니다.")
                .model("GPT-4O-MINI")
                .build();
        }
    }

    private List<String> parseQuestions(String aiResponse) {
        List<String> questions = new ArrayList<>();
        String[] lines = aiResponse.split("\n");
        
        for (String line : lines) {
            String cleaned = line.trim()
                .replaceAll("^\\d+[.)\\s]+", "")
                .replaceAll("^[-*•]\\s+", "")
                .trim();
            
            if (!cleaned.isEmpty() && cleaned.length() > 10 && cleaned.contains("?")) {
                questions.add(cleaned);
            }
        }
        
        return questions;
    }

    private Map<String, Object> parseFeedbackJson(String aiResponse) {
        try {
            String cleanResponse = aiResponse.trim();
            if (cleanResponse.startsWith("```json")) {
                cleanResponse = cleanResponse.substring(7);
            }
            if (cleanResponse.startsWith("```")) {
                cleanResponse = cleanResponse.substring(3);
            }
            if (cleanResponse.endsWith("```")) {
                cleanResponse = cleanResponse.substring(0, cleanResponse.length() - 3);
            }
            cleanResponse = cleanResponse.trim();

            JsonNode jsonNode = objectMapper.readTree(cleanResponse);
            
            Map<String, Object> result = new HashMap<>();
            result.put("score", jsonNode.path("score").asInt(75));
            result.put("strengths", jsonNode.path("strengths").asText("답변이 성실하게 작성되었습니다."));
            result.put("improvements", jsonNode.path("improvements").asText("더 구체적인 사례를 추가하면 좋겠습니다."));
            
            return result;
        } catch (Exception e) {
            log.error("Error parsing feedback JSON: {}", aiResponse, e);
            return generateDummyFeedback("");
        }
    }

    private List<String> generateDummyQuestions(int count) {
        List<String> dummyQuestions = List.of(
            "자기소개를 해주시고, 지원 동기를 말씀해주세요.",
            "본인의 가장 큰 강점은 무엇이며, 그것을 어떻게 업무에 활용하시겠습니까?",
            "최근 진행한 프로젝트에서 가장 어려웠던 문제와 해결 방법을 설명해주세요.",
            "팀원과 의견 충돌이 있었던 경험과 해결 과정을 말씀해주세요.",
            "5년 후 본인의 모습은 어떨 것 같습니까?",
            "본인의 약점은 무엇이며, 이를 극복하기 위해 어떤 노력을 하고 계십니까?",
            "압박 상황에서 업무를 처리했던 경험을 구체적으로 말씀해주세요.",
            "새로운 기술이나 지식을 학습했던 경험과 그 과정을 설명해주세요.",
            "실패했던 경험과 그로부터 배운 점을 말씀해주세요.",
            "우리 회사에 지원한 이유와 기여할 수 있는 부분을 설명해주세요."
        );

        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(count, dummyQuestions.size()); i++) {
            result.add(dummyQuestions.get(i));
        }
        
        while (result.size() < count) {
            result.add("추가 질문 " + (result.size() + 1) + ": 본인에 대해 더 자세히 설명해주세요.");
        }
        
        return result;
    }

    private Map<String, Object> generateDummyFeedback(String answer) {
        int wordCount = answer.split("\\s+").length;
        int score = Math.min(95, 60 + (wordCount / 10));

        Map<String, Object> feedback = new HashMap<>();
        feedback.put("score", score);
        feedback.put("strengths", "답변이 성실하게 작성되었으며, 질문의 핵심을 이해하고 있습니다. 전반적인 구조가 논리적입니다.");
        feedback.put("improvements", "더 구체적인 사례와 수치를 포함하면 답변의 설득력이 높아질 것입니다. STAR 기법을 활용해보세요.");
        
        return feedback;
    }
}
