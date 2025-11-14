package com.mockerview.controller.api;

import com.mockerview.entity.Answer;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.service.AIFeedbackService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@Slf4j
public class FeedbackApiController {

    private final AIFeedbackService aiFeedbackService;
    private final AnswerRepository answerRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @PostMapping("/ai/{answerId}")
    public ResponseEntity<Map<String, Object>> generateAIFeedback(@PathVariable Long answerId) {
        try {
            Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Answer not found"));
            
            Map<String, Object> feedback = aiFeedbackService.generateFeedbackSync(
                answer.getQuestion().getText(),
                answer.getAnswerText()
            );
            
            validateAndClampScore(feedback);
            
            log.info("AI 피드백 생성 완료 - answerId: {}", answerId);
            return ResponseEntity.ok(feedback);
            
        } catch (Exception e) {
            log.error("AI 피드백 생성 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/structured")
    public ResponseEntity<?> generateStructuredFeedback(@RequestBody Map<String, String> request) {
        try {
            String questionText = request.get("questionText");
            String answerText = request.get("answerText");
            String categoryCode = request.get("categoryCode");

            if (questionText == null || answerText == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "질문과 답변을 모두 입력해주세요."
                ));
            }

            Map<String, Object> feedback = aiFeedbackService.generateFeedbackSync(questionText, answerText);
            validateAndClampScore(feedback);
            
            log.info("📊 텍스트 피드백 생성 완료 - 카테고리: {}, 점수: {}", categoryCode, feedback.get("score"));
            return ResponseEntity.ok(feedback);
            
        } catch (Exception e) {
            log.error("텍스트 피드백 생성 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "피드백 생성에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/audio")
    public ResponseEntity<?> generateAudioFeedback(
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam("questionText") String questionText) {
        try {
            if (audioFile.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "음성 파일이 필요합니다."
                ));
            }

            log.info("🎤 음성 피드백 분석 시작 - 파일 크기: {} bytes", audioFile.getSize());

            String transcribedText = transcribeAudio(audioFile);
            
            if (transcribedText == null || transcribedText.trim().isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "score", 1,
                    "summary", "음성 인식에 실패했습니다.",
                    "strengths", "-",
                    "weaknesses", "음성이 명확하게 녹음되지 않았습니다.",
                    "improvements", "더 크고 명확한 목소리로 다시 시도해주세요.",
                    "wpm", 0,
                    "clarity_score", 0,
                    "tone_stability", 0
                ));
            }

            Map<String, Object> feedback = analyzeAudioFeedback(questionText, transcribedText);
            validateAndClampScore(feedback);

            log.info("🎤 음성 피드백 생성 완료 - 점수: {}", feedback.get("score"));
            return ResponseEntity.ok(feedback);
            
        } catch (Exception e) {
            log.error("음성 피드백 생성 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "음성 피드백 생성에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/video")
    public ResponseEntity<?> generateVideoFeedback(
            @RequestParam("video") MultipartFile videoFile,
            @RequestParam("questionText") String questionText) {
        File tempFile = null;
        File audioFile = null;
        
        try {
            if (videoFile.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "영상 파일이 필요합니다."
                ));
            }

            log.info("📹 영상 피드백 분석 시작 - 파일 크기: {} bytes", videoFile.getSize());

            tempFile = File.createTempFile("video_", ".webm");
            videoFile.transferTo(tempFile);

            audioFile = File.createTempFile("audio_", ".webm");
            extractAudioFromVideo(tempFile, audioFile);

            String transcribedText = transcribeAudioFile(audioFile);

            if (transcribedText == null || transcribedText.trim().isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "score", 1,
                    "summary", "영상에서 음성을 인식할 수 없습니다.",
                    "strengths", "-",
                    "weaknesses", "마이크가 음소거되었거나 소리가 녹음되지 않았습니다.",
                    "improvements", "마이크를 켜고 명확하게 말해주세요.",
                    "eye_contact_score", 0,
                    "smile_frequency", 0,
                    "gesture_score", 0,
                    "posture_score", 0
                ));
            }

            Map<String, Object> feedback = analyzeVideoFeedback(questionText, transcribedText, videoFile.getSize());
            validateAndClampScore(feedback);

            log.info("📹 영상 피드백 생성 완료 - 점수: {}", feedback.get("score"));
            return ResponseEntity.ok(feedback);
            
        } catch (Exception e) {
            log.error("영상 피드백 생성 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "영상 피드백 생성에 실패했습니다: " + e.getMessage()
            ));
        } finally {
            if (tempFile != null && tempFile.exists()) tempFile.delete();
            if (audioFile != null && audioFile.exists()) audioFile.delete();
        }
    }

    private void validateAndClampScore(Map<String, Object> feedback) {
        Object scoreObj = feedback.get("score");
        int score = 3;
        
        if (scoreObj instanceof Number) {
            score = ((Number) scoreObj).intValue();
        } else if (scoreObj instanceof String) {
            try {
                score = Integer.parseInt((String) scoreObj);
            } catch (NumberFormatException e) {
                log.warn("점수 파싱 실패, 기본값 3 사용: {}", scoreObj);
            }
        }
        
        score = Math.max(1, Math.min(5, score));
        feedback.put("score", score);
        
        log.info("✅ 점수 검증 완료: {}", score);
    }

    private void extractAudioFromVideo(File videoFile, File outputAudioFile) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
            "ffmpeg",
            "-i", videoFile.getAbsolutePath(),
            "-vn",
            "-acodec", "copy",
            outputAudioFile.getAbsolutePath(),
            "-y"
        );
        
        Process process = pb.start();
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            StringBuilder error = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                error.append(line).append("\n");
            }
            log.warn("FFmpeg 오디오 추출 경고: {}", error.toString());
        }
    }

    private String transcribeAudio(MultipartFile audioFile) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(openaiApiKey);

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

        return (String) response.getBody().get("text");
    }

    private String transcribeAudioFile(File audioFile) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(openaiApiKey);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new org.springframework.core.io.FileSystemResource(audioFile));
        body.add("model", "whisper-1");
        body.add("language", "ko");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "https://api.openai.com/v1/audio/transcriptions",
            requestEntity,
            Map.class
        );

        return (String) response.getBody().get("text");
    }

    private Map<String, Object> analyzeAudioFeedback(String questionText, String transcribedText) throws Exception {
        String prompt = String.format(
            "당신은 면접 전문가입니다. 다음 면접 질문에 대한 음성 답변을 분석해주세요.\n\n" +
            "질문: %s\n\n" +
            "답변 (음성 인식 결과): %s\n\n" +
            "다음 기준으로 평가하고 JSON 형식으로 응답해주세요:\n" +
            "{\n" +
            "  \"score\": 1-5 사이 정수만 (절대 5 초과 금지),\n" +
            "  \"summary\": \"전반적인 평가 요약 (답변 내용 포함)\",\n" +
            "  \"strengths\": \"강점 (답변 내용 + 발음/톤/속도)\",\n" +
            "  \"weaknesses\": \"개선이 필요한 부분 (답변 내용 + 전달력)\",\n" +
            "  \"improvements\": \"구체적인 개선 방안\",\n" +
            "  \"wpm\": 예상 분당 단어 수 (100-200 사이 정수),\n" +
            "  \"clarity_score\": 명확도 점수 0-100,\n" +
            "  \"tone_stability\": 톤 안정성 0-100\n" +
            "}\n\n" +
            "중요: score는 반드시 1, 2, 3, 4, 5 중 하나여야 합니다.\n" +
            "반드시 JSON 형식만 출력하세요. 답변 내용이 질문과 관련 없으면 낮은 점수를 주세요.",
            questionText, transcribedText
        );

        return callGPTForJSON(prompt);
    }

    private Map<String, Object> analyzeVideoFeedback(String questionText, String transcribedText, long videoSize) throws Exception {
        String prompt = String.format(
            "당신은 면접 전문가입니다. 영상 면접 답변을 분석해주세요.\n\n" +
            "질문: %s\n\n" +
            "답변 (영상 음성 인식 결과): %s\n\n" +
            "영상 데이터 크기: %d bytes\n\n" +
            "다음 기준으로 평가하고 JSON 형식으로 응답해주세요:\n" +
            "{\n" +
            "  \"score\": 1-5 사이 정수만 (절대 5 초과 금지),\n" +
            "  \"summary\": \"전반적인 평가 요약 (답변 내용 + 비언어적 요소)\",\n" +
            "  \"strengths\": \"강점 (답변 내용 + 추정되는 표정/태도)\",\n" +
            "  \"weaknesses\": \"개선이 필요한 부분\",\n" +
            "  \"improvements\": \"구체적인 개선 방안 (내용 + 비언어적)\",\n" +
            "  \"eye_contact_score\": 시선 처리 추정 점수 60-95,\n" +
            "  \"smile_frequency\": 미소 빈도 추정 1-5,\n" +
            "  \"gesture_score\": 제스처 활용도 추정 60-95,\n" +
            "  \"posture_score\": 자세 추정 점수 65-95\n" +
            "}\n\n" +
            "중요: score는 반드시 1, 2, 3, 4, 5 중 하나여야 합니다.\n" +
            "반드시 JSON 형식만 출력하세요. 답변 내용이 질문과 관련 없으면 낮은 점수를 주세요.",
            questionText, transcribedText, videoSize
        );

        return callGPTForJSON(prompt);
    }

    private Map<String, Object> callGPTForJSON(String prompt) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o-mini");
        requestBody.put("messages", Arrays.asList(
            Map.of("role", "system", "content", "당신은 면접 평가 전문가입니다. 답변 내용을 정확히 분석하고 반드시 JSON 형식으로만 응답하세요. score는 절대 1-5 범위를 벗어나면 안됩니다."),
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 1000);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + openaiApiKey);
        headers.set("Content-Type", "application/json");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
            "https://api.openai.com/v1/chat/completions",
            request,
            String.class
        );

        JsonNode root = objectMapper.readTree(response.getBody());
        String content = root.path("choices").get(0).path("message").path("content").asText().trim();
        
        content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        
        return objectMapper.readValue(content, Map.class);
    }
}
