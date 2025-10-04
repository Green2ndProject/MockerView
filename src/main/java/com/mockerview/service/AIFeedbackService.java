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
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
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
            
            String prompt = createStructuredFeedbackPrompt(answer);
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

    @Async
    @Transactional
    public void generateMultimodalFeedbackAsync(Long answerId, Long sessionId, String question, String answer, MultipartFile videoFrame) {
        log.info("Starting multimodal AI feedback generation for answer: {}", answerId);
        
        try {
            Answer answerEntity = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Answer not found: " + answerId));
            
            Hibernate.initialize(answerEntity.getQuestion());
            Hibernate.initialize(answerEntity.getUser());
            
            String visualAnalysis = analyzeVideoFrameWithRubric(videoFrame);
            log.info("Visual analysis completed");
            
            String combinedPrompt = createMultimodalStructuredPrompt(question, answer, visualAnalysis);
            String aiResponse = callOpenAI(combinedPrompt);
            
            Feedback feedback = parseMultimodalFeedback(aiResponse, answerEntity);
            feedback = feedbackRepository.save(feedback);
            
            FeedbackMessage feedbackMessage = FeedbackMessage.builder()
                .sessionId(sessionId)
                .answerId(answerId)
                .userId(answerEntity.getUser().getId())
                .summary(feedback.getSummary())
                .strengths(feedback.getStrengths())
                .weaknesses(feedback.getWeaknesses())
                .improvement(feedback.getImprovement())
                .model(feedback.getModel())
                .timestamp(LocalDateTime.now())
                .build();
            
            messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/feedback", feedbackMessage);
            log.info("Multimodal AI feedback completed successfully");
            
        } catch (Exception e) {
            log.error("Error generating multimodal AI feedback: ", e);
            sendErrorFeedback(answerId, sessionId, e.getMessage());
        }
    }

    private String createStructuredFeedbackPrompt(Answer answer) {
        String question = answer.getQuestion().getText();
        String answerText = answer.getAnswerText();
        
        return String.format("""
            당신은 면접 평가 전문가입니다. 다음 답변을 객관적이고 구조화된 기준으로 평가하세요.
            
            [질문]
            %s
            
            [답변]
            %s
            
            [평가 루브릭 - 총 100점]
            
            1. STAR 구조 (30점)
            - Situation (상황): 구체적인 배경과 맥락 제시 (8점)
            - Task (과제): 명확한 목표와 역할 설명 (7점)
            - Action (행동): 구체적인 행동과 과정 서술 (8점)
            - Result (결과): 정량적/정성적 성과 제시 (7점)
            
            2. 내용의 완성도 (25점)
            - 질문에 대한 직접적 답변 (10점)
            - 논리적 흐름과 일관성 (8점)
            - 충분한 설명과 근거 (7점)
            
            3. 구체성 (25점)
            - 실제 경험 기반 사례 (10점)
            - 수치/데이터 활용 (8점)
            - 상세한 과정 묘사 (7점)
            
            4. 전문성 (20점)
            - 분야 이해도 (10점)
            - 전문 용어 적절한 사용 (10점)
            
            [평가 결과를 JSON 형식으로 제공]
            {
                "summary": "전체 평가 요약 (50자)",
                "totalScore": 85,
                "rubric": {
                    "star": {"score": 24, "detail": "STAR 구조 평가"},
                    "completeness": {"score": 20, "detail": "완성도 평가"},
                    "specificity": {"score": 21, "detail": "구체성 평가"},
                    "expertise": {"score": 18, "detail": "전문성 평가"}
                },
                "strengths": "강점 3가지를 구체적으로 (루브릭 항목 기반)",
                "weaknesses": "약점 2-3가지를 건설적으로 (루브릭 항목 기반)",
                "improvement": "STAR 기법과 루브릭 기준에 따른 구체적 개선 방안"
            }
            
            모든 평가는 루브릭 점수 기준으로 객관적으로 작성하세요.
            """, question, answerText);
    }

    private String analyzeVideoFrameWithRubric(MultipartFile videoFrame) {
        try {
            byte[] imageBytes = videoFrame.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey.replace("Bearer ", ""));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o");
            requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "당신은 면접 비언어 커뮤니케이션 전문가입니다. 객관적 루브릭 기준으로 평가하세요."),
                Map.of("role", "user", "content", List.of(
                    Map.of("type", "text", "text", """
                        이 면접자를 다음 루브릭 기준으로 평가하세요 (총 40점):
                        
                        1. 표정 (10점)
                        - 밝고 자연스러운 미소 (4점)
                        - 진지함과 관심 표현 (3점)
                        - 자신감 있는 표정 (3점)
                        
                        2. 자세 (10점)
                        - 바른 자세 유지 (5점)
                        - 편안하고 안정적인 몸가짐 (5점)
                        
                        3. 시선 처리 (10점)
                        - 카메라 직접 응시 (5점)
                        - 안정적인 시선 유지 (5점)
                        
                        4. 전반적 인상 (10점)
                        - 전문성 있는 외모 (5점)
                        - 열정과 진정성 (5점)
                        
                        각 항목별 점수와 구체적 근거를 제공하세요.
                        """),
                    Map.of("type", "image_url", "image_url", Map.of("url", "data:image/jpeg;base64," + base64Image))
                ))
            ));
            requestBody.put("max_tokens", 600);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);
            
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            return jsonNode.path("choices").get(0).path("message").path("content").asText();
            
        } catch (Exception e) {
            log.error("Error analyzing video frame: ", e);
            return "비언어 분석 실패: 표정(0/10), 자세(0/10), 시선(0/10), 인상(0/10)";
        }
    }

    private String createMultimodalStructuredPrompt(String question, String answer, String visualAnalysis) {
        return String.format("""
            당신은 면접 종합 평가 전문가입니다. 언어적 내용과 비언어적 요소를 통합 평가하세요.
            
            [질문]
            %s
            
            [답변 내용]
            %s
            
            [비언어적 분석 (40점)]
            %s
            
            [언어적 평가 루브릭 - 100점]
            1. STAR 구조 (30점)
            2. 내용 완성도 (25점)
            3. 구체성 (25점)
            4. 전문성 (20점)
            
            [종합 평가 = 언어(70%%) + 비언어(30%%)]
            
            JSON 형식으로 제공:
            {
                "summary": "언어+비언어 종합 평가 (70자)",
                "totalScore": 88,
                "verbalScore": 85,
                "nonverbalScore": 32,
                "rubric": {
                    "star": {"score": 25, "detail": "STAR 평가"},
                    "completeness": {"score": 22, "detail": "완성도"},
                    "specificity": {"score": 20, "detail": "구체성"},
                    "expertise": {"score": 18, "detail": "전문성"},
                    "expression": {"score": 8, "detail": "표정"},
                    "posture": {"score": 8, "detail": "자세"},
                    "eyeContact": {"score": 9, "detail": "시선"},
                    "impression": {"score": 7, "detail": "인상"}
                },
                "strengths": "언어적·비언어적 강점 3가지 (루브릭 근거)",
                "weaknesses": "개선 필요 부분 2-3가지 (루브릭 근거)",
                "improvement": "STAR 기법 + 비언어 개선 구체적 방안"
            }
            
            모든 평가는 루브릭 점수를 명시하며 객관적으로 작성하세요.
            """, question, answer, visualAnalysis);
    }

    public List<String> generateInterviewQuestions(int count) {
        log.info("Generating {} interview questions", count);
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("OpenAI API key not configured, returning diverse dummy questions");
            return generateDiverseDummyQuestions(count);
        }

        try {
            String prompt = String.format("""
                면접 질문 %d개를 생성해주세요.
                
                요구사항:
                1. 각 질문은 서로 완전히 다른 주제여야 합니다
                2. STAR 기법으로 답변 가능한 행동 기반 질문
                3. 번호나 특수문자 없이 질문만 작성
                4. 다음 카테고리를 골고루 포함:
                    - 자기소개/경력 (1-2개)
                    - 문제 해결 능력 (2-3개)
                    - 협업/커뮤니케이션 (1-2개)
                    - 기술 역량 (1-2개)
                    - 성장 마인드 (1개)
                
                형식: 각 줄에 하나의 질문만 작성
                """, count);

            String aiResponse = callOpenAI(prompt);
            log.info("OpenAI raw response for questions: {}", aiResponse);
            
            List<String> questions = parseQuestions(aiResponse);
            
            if (questions.isEmpty()) {
                log.warn("OpenAI returned empty questions, using diverse dummy questions");
                return generateDiverseDummyQuestions(count);
            }
            
            log.info("Successfully generated {} unique questions", questions.size());
            questions.forEach(q -> log.info("Generated question: {}", q));
            
            while (questions.size() < count) {
                questions.add(String.format("추가 질문 %d: 본인의 강점과 약점에 대해 구체적 사례와 함께 말씀해주세요.", questions.size() + 1));
            }

            return questions.subList(0, Math.min(count, questions.size()));
            
        } catch (Exception e) {
            log.error("Error generating interview questions, using diverse dummy questions: ", e);
            return generateDiverseDummyQuestions(count);
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
                다음 답변을 루브릭 기준으로 평가하세요.
                
                [질문]
                %s
                
                [답변]
                %s
                
                [루브릭 - 100점]
                1. STAR 구조 (30점): S, T, A, R 각 요소 포함 여부
                2. 내용 완성도 (25점): 질문 직접 답변, 논리성, 근거
                3. 구체성 (25점): 실제 사례, 수치, 상세 과정
                4. 전문성 (20점): 분야 이해도, 용어 사용
                
                JSON 형식:
                {
                    "score": 85,
                    "rubric": {"star": 24, "completeness": 21, "specificity": 22, "expertise": 18},
                    "strengths": "강점 (루브릭 항목 명시)",
                    "improvements": "개선점 (루브릭 항목 명시)"
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
            Map.of("role", "system", "content", "당신은 루브릭 기반 객관적 면접 평가 전문가입니다. 모든 평가는 명확한 점수 기준과 근거를 제시합니다."),
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("max_tokens", 1000);
        requestBody.put("temperature", 0.3);

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

    private Feedback parseMultimodalFeedback(String aiResponse, Answer answer) {
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
                .summary(feedback.path("summary").asText("멀티모달 루브릭 분석이 완료되었습니다."))
                .strengths(feedback.path("strengths").asText("언어와 비언어 요소가 양호합니다."))
                .weaknesses(feedback.path("weaknesses").asText("일부 개선이 필요합니다."))
                .improvement(feedback.path("improvement").asText("STAR 구조와 표정을 함께 개선해보세요."))
                .model("GPT-4O")
                .build();
                
        } catch (Exception e) {
            log.error("Error parsing multimodal feedback: ", e);
            
            return Feedback.builder()
                .answer(answer)
                .summary("멀티모달 루브릭 분석이 완료되었습니다.")
                .strengths("답변이 제출되었습니다.")
                .weaknesses("분석 중 일부 오류가 발생했습니다.")
                .improvement("다음 답변에서 더 나은 결과를 기대합니다.")
                .model("GPT-4O")
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
            result.put("strengths", jsonNode.path("strengths").asText("루브릭 기준 답변이 성실하게 작성되었습니다."));
            result.put("improvements", jsonNode.path("improvements").asText("STAR 구조와 구체성을 강화하면 좋겠습니다."));
            
            return result;
        } catch (Exception e) {
            log.error("Error parsing feedback JSON: {}", aiResponse, e);
            return generateDummyFeedback("");
        }
    }

    private List<String> generateDiverseDummyQuestions(int count) {
        List<String> pool = List.of(
            "최근 프로젝트에서 직면한 가장 큰 도전과제는 무엇이었으며, 어떻게 해결하셨나요?",
            "팀 내에서 의견 충돌이 있었던 경험을 STAR 기법으로 설명해주세요.",
            "본인의 리더십을 발휘했던 구체적인 사례를 말씀해주세요.",
            "실패를 극복하고 성공으로 전환시킨 경험이 있다면 말씀해주세요.",
            "새로운 기술이나 도구를 빠르게 습득했던 경험과 학습 방법을 설명해주세요.",
            "고객이나 이해관계자의 어려운 요구사항을 처리했던 경험을 공유해주세요.",
            "시간 압박 속에서 품질을 유지하며 업무를 완료했던 사례를 설명해주세요.",
            "팀의 성과 향상을 위해 주도적으로 개선한 경험이 있나요?",
            "복잡한 문제를 단순화하여 해결했던 경험을 구체적으로 말씀해주세요.",
            "본인의 약점을 인식하고 개선하기 위해 노력한 사례를 설명해주세요.",
            "다양한 배경을 가진 팀원들과 협업했던 경험을 설명해주세요.",
            "우선순위가 충돌하는 여러 업무를 동시에 관리했던 경험은?",
            "예상치 못한 변화나 위기 상황에 대처했던 사례를 말씀해주세요.",
            "다른 사람을 설득하거나 영향을 미쳤던 경험을 공유해주세요.",
            "데이터나 분석을 활용해 의사결정을 개선했던 사례가 있나요?"
        );

        List<String> shuffled = new ArrayList<>(pool);
        java.util.Collections.shuffle(shuffled);
        
        List<String> result = shuffled.subList(0, Math.min(count, shuffled.size()));
        
        while (result.size() < count) {
            result.add(String.format("보충 질문 %d: 본인의 경험을 STAR 기법으로 구체적으로 설명해주세요.", result.size() + 1));
        }
        
        log.info("Returning {} diverse dummy questions (shuffled)", result.size());
        return result;
    }

    private Map<String, Object> generateDummyFeedback(String answer) {
        int wordCount = answer.split("\\s+").length;
        int score = Math.min(95, 60 + (wordCount / 10));

        Map<String, Object> feedback = new HashMap<>();
        feedback.put("score", score);
        feedback.put("strengths", "[STAR 구조: 20/30] 상황과 결과가 명확. [구체성: 18/25] 실제 경험 기반. [전문성: 16/20] 용어 적절.");
        feedback.put("improvements", "[STAR: -6점] Task 명확화 필요. [구체성: -7점] 수치 데이터 추가. STAR 전 요소 포함 시 85점 이상 가능.");
        
        return feedback;
    }
}