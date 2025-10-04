package com.mockerview.controller.api;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.*;
import com.mockerview.repository.*;
import com.mockerview.service.SelfInterviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/selfinterview")
@RequiredArgsConstructor
@Slf4j
public class SelfInterviewApiController {

    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final SelfInterviewService selfInterviewService;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${openai.api.key}")
    private String openaiApiKey;
    
    @Value("${openai.api.url}")
    private String openaiApiUrl;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createSelfInterview(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long userId = userDetails.getUserId();
            String title = (String) request.get("title");
            Integer questionCount = (Integer) request.get("questionCount");
            
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Session session = selfInterviewService.createSelfInterviewSession(
                user, title, questionCount);
            
            response.put("success", true);
            response.put("message", "셀프 면접이 생성되었습니다");
            response.put("sessionId", session.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("셀프 면접 생성 실패", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSessionData(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Session session = sessionRepository.findById(sessionId).orElse(null);
        
        if (session == null || !session.getHost().getId().equals(userDetails.getUserId())) {
            log.warn("Session not found or unauthorized - sessionId: {}, userId: {}", sessionId, userDetails.getUserId());
            return ResponseEntity.notFound().build();
        }

        List<Question> questions = questionRepository.findBySessionIdOrderByOrderNo(sessionId);
        
        List<Map<String, Object>> questionMaps = questions.stream().map(q -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", q.getId());
            map.put("questionText", q.getText());
            map.put("orderNumber", q.getOrderNo());
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getId());
        response.put("userId", userDetails.getUserId());
        response.put("title", session.getTitle());
        response.put("questions", questionMaps);

        log.info("Session data sent - sessionId: {}, questions: {}", sessionId, questionMaps.size());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/transcribe")
    public ResponseEntity<Map<String, Object>> transcribeAudio(
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam("questionId") Long questionId,
            @RequestParam("sessionId") Long sessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            log.info("음성 파일 수신 - 크기: {} bytes", audioFile.getSize());

            String transcribedText = transcribeWithWhisper(audioFile);
            log.info("음성 변환 완료: {}", transcribedText.substring(0, Math.min(50, transcribedText.length())));

            Question question = questionRepository.findById(questionId).orElseThrow();
            User user = userRepository.findById(userDetails.getUserId()).orElseThrow();

            Answer answer = Answer.builder()
                    .question(question)
                    .user(user)
                    .answerText(transcribedText)
                    .build();
            answer = answerRepository.save(answer);

            String feedbackText = generateAIFeedback(question.getText(), transcribedText);
            
            int score = extractScore(feedbackText);
            String strengths = extractSection(feedbackText, "강점");
            String improvements = extractSection(feedbackText, "개선점");

            Feedback feedback = Feedback.builder()
                    .answer(answer)
                    .feedbackType(Feedback.FeedbackType.AI)
                    .summary("AI 음성 분석 완료")
                    .score(score)
                    .strengths(strengths)
                    .weaknesses("")
                    .improvement(improvements)
                    .model("GPT-4O-MINI")
                    .build();
            feedbackRepository.save(feedback);

            Map<String, Object> result = new HashMap<>();
            result.put("answer", Map.of(
                "id", answer.getId(), 
                "answerText", transcribedText
            ));
            result.put("feedback", Map.of(
                "score", score,
                "strengths", strengths,
                "improvements", improvements
            ));

            log.info("음성 답변 처리 완료 - answerId: {}", answer.getId());

            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("음성 처리 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{sessionId}/answer")
    public ResponseEntity<Map<String, Object>> submitAnswer(
            @PathVariable Long sessionId,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            Long questionId = Long.valueOf(request.get("questionId").toString());
            String answerText = request.get("answerText").toString();

            log.info("Answer submitted - sessionId: {}, questionId: {}, userId: {}", 
                    sessionId, questionId, userDetails.getUserId());

            Question question = questionRepository.findById(questionId).orElseThrow();
            User user = userRepository.findById(userDetails.getUserId()).orElseThrow();

            Answer answer = Answer.builder()
                    .question(question)
                    .user(user)
                    .answerText(answerText)
                    .build();
            answer = answerRepository.save(answer);

            String feedbackText = generateAIFeedback(question.getText(), answerText);
            
            int score = extractScore(feedbackText);
            String strengths = extractSection(feedbackText, "강점");
            String improvements = extractSection(feedbackText, "개선점");

            Feedback feedback = Feedback.builder()
                    .answer(answer)
                    .feedbackType(Feedback.FeedbackType.AI)
                    .summary("AI 텍스트 분석 완료")
                    .score(score)
                    .strengths(strengths)
                    .weaknesses("")
                    .improvement(improvements)
                    .model("GPT-4O-MINI")
                    .build();
            feedbackRepository.save(feedback);

            Map<String, Object> result = new HashMap<>();
            result.put("answer", Map.of("id", answer.getId(), "answerText", answerText));
            result.put("feedback", Map.of(
                "score", score,
                "strengths", strengths,
                "improvements", improvements
            ));

            log.info("Feedback generated - score: {}", score);

            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Failed to submit answer", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{sessionId}/video-answer")
    public ResponseEntity<Map<String, Object>> submitVideoAnswer(
            @PathVariable Long sessionId,
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam("questionId") Long questionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            log.info("비디오 답변 수신 - 크기: {} bytes", audioFile.getSize());

            String transcribedText = transcribeWithWhisper(audioFile);
            log.info("음성 변환 완료: {}", transcribedText.substring(0, Math.min(50, transcribedText.length())));

            Question question = questionRepository.findById(questionId).orElseThrow();
            User user = userRepository.findById(userDetails.getUserId()).orElseThrow();

            Answer answer = Answer.builder()
                    .question(question)
                    .user(user)
                    .answerText(transcribedText)
                    .build();
            answer = answerRepository.save(answer);

            String feedbackText = generateAIFeedback(question.getText(), transcribedText);
            
            int score = extractScore(feedbackText);
            String strengths = extractSection(feedbackText, "강점");
            String improvements = extractSection(feedbackText, "개선점");

            Feedback feedback = Feedback.builder()
                    .answer(answer)
                    .feedbackType(Feedback.FeedbackType.AI)
                    .summary("AI 비디오 분석 완료")
                    .score(score)
                    .strengths(strengths)
                    .weaknesses("")
                    .improvement(improvements)
                    .model("GPT-4O-MINI")
                    .build();
            feedbackRepository.save(feedback);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("transcription", transcribedText);
            result.put("answer", Map.of("id", answer.getId(), "answerText", transcribedText));
            result.put("feedback", Map.of(
                "score", score,
                "strengths", strengths,
                "improvements", improvements
            ));

            log.info("비디오 답변 처리 완료 - answerId: {}", answer.getId());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("비디오 답변 처리 실패", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "비디오 처리 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    private String transcribeWithWhisper(MultipartFile audioFile) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(openaiApiKey.replace("Bearer ", ""));
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", audioFile.getResource());
            body.add("model", "whisper-1");
            body.add("language", "ko");

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.openai.com/v1/audio/transcriptions",
                requestEntity,
                Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            return (String) responseBody.get("text");

        } catch (Exception e) {
            log.error("Whisper API 호출 실패", e);
            return "음성 인식에 실패했습니다.";
        }
    }

    private String generateAIFeedback(String questionText, String answerText) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o-mini");
            requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "당신은 면접 평가 전문가입니다. 답변을 평가하고 피드백을 제공하세요."),
                Map.of("role", "user", "content", String.format("질문: %s\n\n답변: %s\n\n위 답변을 평가하고 다음 형식으로 피드백을 제공해주세요:\n점수: (0-100)\n강점: (구체적인 강점)\n개선점: (구체적인 개선점)", questionText, answerText))
            ));
            requestBody.put("max_tokens", 500);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + openaiApiKey);
            headers.set("Content-Type", "application/json");

            HttpEntity<Map<String, Object>> entity = 
                new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                openaiApiUrl, entity, Map.class);

            Map<String, Object> responseBody = response.getBody();
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            
            return (String) message.get("content");
        } catch (Exception e) {
            log.error("AI feedback generation failed", e);
            return "점수: 75\n강점: 질문에 대한 답변을 제공했습니다.\n개선점: 더 구체적인 예시와 함께 답변하면 좋습니다.";
        }
    }

    private int extractScore(String feedback) {
        try {
            if (feedback.contains("점수:")) {
                String scorePart = feedback.substring(feedback.indexOf("점수:") + 3).trim();
                String scoreStr = scorePart.split("[^0-9]")[0];
                return Integer.parseInt(scoreStr);
            }
        } catch (Exception e) {
            log.warn("Failed to extract score", e);
        }
        return 75;
    }

    private String extractSection(String feedback, String section) {
        try {
            if (feedback.contains(section + ":")) {
                String[] parts = feedback.split(section + ":");
                if (parts.length > 1) {
                    String content = parts[1].trim();
                    int nextSection = content.indexOf("\n\n");
                    if (nextSection > 0) {
                        content = content.substring(0, nextSection);
                    }
                    return content.trim();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract section: " + section, e);
        }
        return "분석 중...";
    }
}