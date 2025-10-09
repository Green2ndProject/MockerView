package com.mockerview.controller.api;

import com.mockerview.dto.AnswerMessage;
import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.Answer;
import com.mockerview.entity.Feedback;
import com.mockerview.entity.Question;
import com.mockerview.entity.Session;
import com.mockerview.entity.User;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.FeedbackRepository;
import com.mockerview.repository.QuestionRepository;
import com.mockerview.repository.SessionRepository;
import com.mockerview.repository.UserRepository;
import com.mockerview.service.AIFeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
@Slf4j
public class SessionApiController {

    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final FeedbackRepository feedbackRepository;
    private final AIFeedbackService aiFeedbackService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${openai.api.key}")
    private String openaiApiKey;
    
    @Value("${openai.api.url}")
    private String openaiApiUrl;

    @GetMapping("/{sessionId}/info")
    public ResponseEntity<Map<String, Object>> getSessionInfo(@PathVariable Long sessionId) {
        try {
            Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
            
            List<Question> questions = questionRepository.findBySessionIdOrderByOrderNo(sessionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", session.getId());
            response.put("title", session.getTitle());
            response.put("status", session.getStatus());
            response.put("questionCount", questions.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get session info", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{sessionId}/voice-answer")
    public ResponseEntity<Map<String, Object>> submitVoiceAnswer(
            @PathVariable Long sessionId,
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam("questionId") Long questionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            log.info("음성 답변 수신 - sessionId: {}, questionId: {}, 파일 크기: {} bytes", 
                    sessionId, questionId, audioFile.getSize());

            String transcribedText = transcribeWithWhisper(audioFile);
            log.info("음성 변환 완료: {}", transcribedText.substring(0, Math.min(50, transcribedText.length())));
            
            Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
            User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

            Answer answer = Answer.builder()
                .question(question)
                .user(user)
                .answerText(transcribedText)
                .build();
            answer = answerRepository.save(answer);

            AnswerMessage answerMessage = AnswerMessage.builder()
                .sessionId(sessionId)
                .answerId(answer.getId())
                .questionId(questionId)
                .userId(user.getId())
                .userName(user.getName())
                .answerText(transcribedText)
                .timestamp(LocalDateTime.now())
                .build();

            messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/answer", answerMessage);
            
            aiFeedbackService.generateFeedbackAsync(answer.getId(), sessionId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("answerId", answer.getId());
            result.put("transcription", transcribedText);

            log.info("음성 답변 처리 완료 - answerId: {}", answer.getId());

            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("음성 답변 처리 실패", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/{sessionId}/video-answer")
    public ResponseEntity<Map<String, Object>> submitVideoAnswer(
            @PathVariable Long sessionId,
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam(value = "videoFrame", required = false) MultipartFile videoFrame,
            @RequestParam("questionId") Long questionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            log.info("비디오 답변 수신 - sessionId: {}, questionId: {}", sessionId, questionId);

            String transcribedText = transcribeWithWhisper(audioFile);
            
            Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
            User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

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

            AnswerMessage answerMessage = AnswerMessage.builder()
                .sessionId(sessionId)
                .answerId(answer.getId())
                .questionId(questionId)
                .userId(user.getId())
                .userName(user.getName())
                .answerText(transcribedText)
                .timestamp(LocalDateTime.now())
                .build();

            messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/answer", answerMessage);
            messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/feedback", 
                Map.of("answerId", answer.getId(), "feedback", feedback));

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("answerId", answer.getId());
            result.put("transcription", transcribedText);

            log.info("비디오 답변 처리 완료 - answerId: {}", answer.getId());

            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("비디오 답변 처리 실패", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/{sessionId}/end")
    public ResponseEntity<Map<String, Object>> endSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("세션 종료 요청 - sessionId: {}, userId: {}", 
                    sessionId, userDetails.getUserId());
            
            if (!sessionRepository.isHost(sessionId, userDetails.getUserId())) {
                response.put("success", false);
                response.put("message", "호스트만 세션을 종료할 수 있습니다");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
            
            if (session.getSessionStatus() == Session.SessionStatus.ENDED) {
                response.put("success", false);
                response.put("message", "이미 종료된 세션입니다");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
            
            session.setSessionStatus(Session.SessionStatus.ENDED);
            session.setEndTime(LocalDateTime.now());
            sessionRepository.save(session);
            
            Map<String, Object> statusMessage = new HashMap<>();
            statusMessage.put("sessionId", sessionId);
            statusMessage.put("status", "ENDED");
            statusMessage.put("reason", "MANUAL");
            statusMessage.put("timestamp", LocalDateTime.now());
            
            messagingTemplate.convertAndSend(
                "/topic/session/" + sessionId + "/status", 
                statusMessage
            );
            
            response.put("success", true);
            response.put("message", "세션이 종료되었습니다");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("세션 종료 실패", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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