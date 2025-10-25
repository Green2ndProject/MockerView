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
import com.mockerview.service.SessionService;
import com.mockerview.service.AdvancedVoiceAnalysisService;
import com.mockerview.service.FacialAnalysisService;

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
    private final SessionService sessionService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final AdvancedVoiceAnalysisService voiceAnalysisService;
    private final FacialAnalysisService facialAnalysisService;
    
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
                .improvementSuggestions(improvements)
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
            
            voiceAnalysisService.analyzeVoiceAsync(answer.getId(), audioFile, transcribedText);
            log.info("✅ 음성 분석 시작됨 - answerId: {}", answer.getId());

            Map<String, Object> answerData = new HashMap<>();
            answerData.put("id", answer.getId());
            answerData.put("text", transcribedText);

            Map<String, Object> feedbackData = new HashMap<>();
            feedbackData.put("score", feedback.getScore());
            feedbackData.put("strengths", feedback.getStrengths());
            feedbackData.put("improvements", feedback.getImprovementSuggestions());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("answer", answerData);
            result.put("feedback", feedbackData);

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

    @PostMapping(value = "/{sessionId}/video-answer", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
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
                .improvementSuggestions(improvements)
                .build();
            feedbackRepository.save(feedback);

            voiceAnalysisService.analyzeVoiceAsync(answer.getId(), audioFile, transcribedText);
            log.info("✅ 음성 분석 시작됨 - answerId: {}", answer.getId());
            
            if (videoFrame != null && !videoFrame.isEmpty()) {
                facialAnalysisService.analyzeFaceAsync(answer.getId(), videoFrame);
                log.info("✅ 표정 분석 시작됨 - answerId: {}", answer.getId());
            }

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

            Map<String, Object> answerData = new HashMap<>();
            answerData.put("id", answer.getId());
            answerData.put("text", transcribedText);

            Map<String, Object> feedbackData = new HashMap<>();
            feedbackData.put("score", feedback.getScore());
            feedbackData.put("strengths", feedback.getStrengths());
            feedbackData.put("improvements", feedback.getImprovementSuggestions());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("answer", answerData);
            result.put("feedback", feedbackData);

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

    @PostMapping("/{sessionId}/start")
    public ResponseEntity<Map<String, String>> startSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Map<String, String> response = new HashMap<>();
        
        try {
            log.info("✅ 세션 시작 요청 - sessionId: {}, userId: {}", 
                    sessionId, userDetails.getUserId());
            
            if (!sessionRepository.isHost(sessionId, userDetails.getUserId())) {
                response.put("status", "error");
                response.put("message", "호스트만 세션을 시작할 수 있습니다");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
            
            if (session.getSessionStatus() == Session.SessionStatus.RUNNING) {
                response.put("status", "error");
                response.put("message", "이미 진행 중인 세션입니다");
                return ResponseEntity.ok(response);
            }
            
            session.setSessionStatus(Session.SessionStatus.RUNNING);
            session.setStartTime(LocalDateTime.now());
            sessionRepository.save(session);
            
            log.info("✅ 세션 시작 완료 - sessionId: {}", sessionId);
            
            Map<String, Object> statusMessage = new HashMap<>();
            statusMessage.put("sessionId", sessionId);
            statusMessage.put("status", "RUNNING");
            statusMessage.put("timestamp", LocalDateTime.now());
            
            messagingTemplate.convertAndSend(
                "/topic/session/" + sessionId + "/status", 
                statusMessage
            );

            response.put("status", "success");
            response.put("message", "세션이 시작되었습니다");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ 세션 시작 실패: ", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{sessionId}/status")
    public ResponseEntity<Map<String, Object>> getSessionStatus(@PathVariable Long sessionId) {
        try {
            Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", session.getStatus().name());
            response.put("sessionId", sessionId);
            
            log.info("세션 상태 조회 - sessionId: {}, status: {}", sessionId, session.getStatus());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("세션 상태 조회 실패: {}", sessionId, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{sessionId}/ai/toggle")
    public ResponseEntity<?> toggleAI(
        @PathVariable Long sessionId,
        @RequestBody Map<String, Boolean> request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Boolean enabled = request.get("enabled");
            log.info("🔄 AI 토글 요청 - sessionId: {}, enabled: {}, user: {}", 
                    sessionId, enabled, userDetails.getUsername());
            
            sessionService.toggleAI(sessionId, enabled, userDetails.getUsername());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "enabled", enabled,
                "message", enabled ? "AI 활성화됨" : "AI 비활성화됨"
            ));
        } catch (Exception e) {
            log.error("❌ AI 토글 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    @PostMapping("/{sessionId}/ai/mode")
    public ResponseEntity<?> changeAiMode(
        @PathVariable Long sessionId,
        @RequestBody Map<String, String> request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            String mode = request.get("mode");
            log.info("🔄 AI 모드 변경 요청 - sessionId: {}, mode: {}, user: {}", 
                    sessionId, mode, userDetails.getUsername());
            
            sessionService.updateAiMode(sessionId, mode, userDetails.getUsername());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "mode", mode,
                "message", "AI 모드 변경됨"
            ));
        } catch (Exception e) {
            log.error("❌ AI 모드 변경 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    @PostMapping("/{sessionId}/upload-video")
    public ResponseEntity<?> uploadVideo(
        @PathVariable Long sessionId,
        @RequestParam("video") MultipartFile videoFile
    ) {
        try {
            Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

            String cloudName = System.getenv("CLOUDINARY_CLOUD_NAME");
            String apiKey = System.getenv("CLOUDINARY_API_KEY");
            String apiSecret = System.getenv("CLOUDINARY_API_SECRET");

            if (cloudName == null || apiKey == null || apiSecret == null) {
                log.error("Cloudinary 환경 변수 누락");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Cloudinary 설정 오류"));
            }

            String uploadUrl = "https://api.cloudinary.com/v1_1/" + cloudName + "/video/upload";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", videoFile.getResource());
            body.add("upload_preset", "mockerview");
            body.add("folder", "interview_recordings");

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            Map response = restTemplate.postForObject(uploadUrl, requestEntity, Map.class);

            if (response != null && response.containsKey("secure_url")) {
                String videoUrl = (String) response.get("secure_url");
                session.setVideoRecordingUrl(videoUrl);
                sessionRepository.save(session);

                log.info("Video uploaded successfully: {}", videoUrl);

                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "videoUrl", videoUrl
                ));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Cloudinary 업로드 실패"));
            }

        } catch (Exception e) {
            log.error("Video upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{sessionId}/start-recording")
    public ResponseEntity<?> startRecording(@PathVariable Long sessionId) {
        try {
            Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

            session.setStartTime(LocalDateTime.now());
            session.setSessionStatus(Session.SessionStatus.RUNNING);
            sessionRepository.save(session);

            log.info("Recording started for session: {}", sessionId);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Failed to start recording", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    @GetMapping("/{sessionId}/ai/status")
    public ResponseEntity<?> getAiStatus(@PathVariable Long sessionId) {
        try {
            Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
            
            log.info("📊 AI 상태 조회 - sessionId: {}, enabled: {}, mode: {}", 
                    sessionId, session.getAiEnabled(), session.getAiMode());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "enabled", session.getAiEnabled() != null ? session.getAiEnabled() : true,
                "mode", session.getAiMode() != null ? session.getAiMode() : "FULL",
                "allowParticipantsToggle", session.getAllowParticipantsToggleAi() != null ? 
                    session.getAllowParticipantsToggleAi() : false
            ));
        } catch (Exception e) {
            log.error("❌ AI 상태 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{sessionId}/ai-toggle")
    public ResponseEntity<?> toggleAI(@PathVariable Long sessionId, @RequestBody Map<String, Boolean> request) {
        try {
            Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
            
            Boolean enabled = request.get("enabled");
            session.setAiEnabled(enabled);
            sessionRepository.save(session);
            
            log.info("AI 토글: sessionId={}, enabled={}", sessionId, enabled);
            
            return ResponseEntity.ok(Map.of("success", true, "enabled", enabled));
        } catch (Exception e) {
            log.error("AI 토글 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{sessionId}/ai-feedback")
    public ResponseEntity<?> requestAIFeedback(
            @PathVariable Long sessionId,
            @RequestBody Map<String, Object> request) {
        try {
            log.info("🤖 AI 피드백 요청 - sessionId: {}", sessionId);
            
            List<Map<String, Object>> qaList = (List<Map<String, Object>>) request.get("qaList");
            
            if (qaList == null || qaList.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "QA 목록이 비어있습니다"));
            }

            StringBuilder context = new StringBuilder();
            for (Map<String, Object> qa : qaList) {
                String type = (String) qa.get("type");
                String content = (String) qa.get("content");
                String author = (String) qa.get("author");
                
                if ("question".equals(type)) {
                    context.append("질문 (").append(author).append("): ").append(content).append("\n");
                } else if ("answer".equals(type)) {
                    context.append("답변 (").append(author).append("): ").append(content).append("\n");
                }
            }

            String feedbackText = generateAIFeedback("종합 평가", context.toString());
            
            int score = extractScore(feedbackText);
            String strengths = extractSection(feedbackText, "강점");
            String weaknesses = extractSection(feedbackText, "약점");
            String improvements = extractSection(feedbackText, "개선");

            Map<String, Object> feedback = new HashMap<>();
            feedback.put("score", score);
            feedback.put("strengths", strengths != null ? strengths : "분석 중...");
            feedback.put("weaknesses", weaknesses != null ? weaknesses : "분석 중...");
            feedback.put("improvements", improvements != null ? improvements : "분석 중...");

            log.info("✅ AI 피드백 생성 완료");

            return ResponseEntity.ok(feedback);
            
        } catch (Exception e) {
            log.error("❌ AI 피드백 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{sessionId}/feedback")
    public ResponseEntity<?> submitFeedback(
            @PathVariable Long sessionId,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            log.info("📝 피드백 제출 시작 - sessionId: {}, interviewer: {}, request: {}", 
                sessionId, userDetails.getUserId(), request);
            
            Long intervieweeId = Long.valueOf(request.get("intervieweeId").toString());
            Integer rating = Integer.valueOf(request.get("rating").toString());
            String strengths = (String) request.get("strengths");
            String weaknesses = (String) request.get("weaknesses");
            String improvements = (String) request.get("improvements");
            String notes = (String) request.get("notes");

            User interviewee = userRepository.findById(intervieweeId)
                .orElseThrow(() -> new RuntimeException("면접자를 찾을 수 없습니다"));

            Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다"));

            List<Answer> answers = answerRepository.findByQuestionSessionIdAndUserId(sessionId, intervieweeId);
            
            log.info("📊 답변 조회 결과 - sessionId: {}, intervieweeId: {}, 답변 수: {}", 
                sessionId, intervieweeId, answers.size());
            
            if (answers.isEmpty()) {
                log.warn("⚠️ 답변이 없습니다 - sessionId: {}, intervieweeId: {}", sessionId, intervieweeId);
                
                List<com.mockerview.entity.Question> questions = questionRepository.findBySessionIdOrderByOrderNo(sessionId);
                log.info("📝 질문 조회 결과 - 질문 수: {}", questions.size());
                
                if (questions.isEmpty()) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "면접 질문이 없습니다. 먼저 질문을 작성해주세요."));
                }
                
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "면접자의 답변이 없습니다. 답변을 기다려주세요."));
            }

            for (Answer answer : answers) {
                Optional<Feedback> existingFeedback = feedbackRepository.findByAnswerIdAndFeedbackType(
                    answer.getId(), Feedback.FeedbackType.INTERVIEWER);
                
                if (existingFeedback.isPresent()) {
                    Feedback feedback = existingFeedback.get();
                    feedback.setSummary(notes != null ? notes : "");
                    feedback.setScore(rating);
                    feedback.setStrengths(strengths != null ? strengths : "");
                    feedback.setWeaknesses(weaknesses != null ? weaknesses : "");
                    feedback.setImprovementSuggestions(improvements != null ? improvements : "");
                    feedbackRepository.save(feedback);
                    log.info("✅ 피드백 업데이트 - answerId: {}", answer.getId());
                } else {
                    Feedback feedback = Feedback.builder()
                        .answer(answer)
                        .feedbackType(Feedback.FeedbackType.INTERVIEWER)
                        .summary(notes != null ? notes : "")
                        .score(rating)
                        .strengths(strengths != null ? strengths : "")
                        .weaknesses(weaknesses != null ? weaknesses : "")
                        .improvementSuggestions(improvements != null ? improvements : "")
                        .build();
                    
                    feedbackRepository.save(feedback);
                    log.info("✅ 피드백 신규 저장 - answerId: {}", answer.getId());
                }
            }

            log.info("✅ 피드백 제출 완료 - {} 개의 답변에 대해", answers.size());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "피드백이 저장되었습니다",
                "feedbackCount", answers.size()
            ));
            
        } catch (Exception e) {
            log.error("❌ 피드백 제출 실패 - sessionId: {}, error: {}", sessionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "피드백 저장에 실패했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/{sessionId}/interviewee/{intervieweeId}/feedback")
    public ResponseEntity<?> getIntervieweeFeedback(
            @PathVariable Long sessionId,
            @PathVariable Long intervieweeId) {
        try {
            log.info("📊 면접자 피드백 조회 - sessionId: {}, intervieweeId: {}", sessionId, intervieweeId);
            
            List<Answer> answers = answerRepository.findByQuestionSessionIdAndUserId(sessionId, intervieweeId);
            
            if (answers.isEmpty()) {
                return ResponseEntity.ok(Map.of("aiFeedback", null));
            }

            Feedback latestAIFeedback = null;
            for (Answer answer : answers) {
                for (Feedback feedback : answer.getFeedbacks()) {
                    if (feedback.getFeedbackType() == Feedback.FeedbackType.AI) {
                        latestAIFeedback = feedback;
                        break;
                    }
                }
                if (latestAIFeedback != null) break;
            }

            if (latestAIFeedback != null) {
                Map<String, Object> aiFeedback = Map.of(
                    "strengths", latestAIFeedback.getStrengths() != null ? latestAIFeedback.getStrengths() : "분석 중...",
                    "weaknesses", latestAIFeedback.getWeaknesses() != null ? latestAIFeedback.getWeaknesses() : "분석 중...",
                    "improvements", latestAIFeedback.getImprovementSuggestions() != null ? latestAIFeedback.getImprovementSuggestions() : "분석 중..."
                );
                
                return ResponseEntity.ok(Map.of("aiFeedback", aiFeedback));
            }

            return ResponseEntity.ok(Map.of("aiFeedback", null));
            
        } catch (Exception e) {
            log.error("❌ 피드백 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
}