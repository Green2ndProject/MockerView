package com.mockerview.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeSubtitleService {

    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate;

    @Value("${openai.api.key}")
    private String openaiApiKey;

    private static final List<String> HALLUCINATION_PATTERNS = Arrays.asList(
        "구독", "좋아요", "알림", "시청", "감사합니다", "재밌게 보셨다면",
        "댓글", "공유", "방송", "뉴스", "전해드렸습니다",
        "통합뉴스", "다음 주에", "만나요", "주에 만나요", "영상"
    );

    public void transcribeAndBroadcast(Long sessionId, Long userId, String userName, MultipartFile audio) {
        try {
            log.info("🎤 자막 생성 시작 - Session: {}, User: {}, Size: {} bytes", 
                sessionId, userName, audio.getSize());
            
            if (audio.getSize() < 5000) {
                log.warn("⚠️ 오디오 파일이 너무 작음: {} bytes", audio.getSize());
                return;
            }

            String transcription = transcribeAudio(audio);
            
            if (transcription == null || transcription.trim().isEmpty()) {
                log.warn("⚠️ 빈 자막 결과");
                return;
            }

            transcription = transcription.trim();

            if (isHallucination(transcription)) {
                log.warn("⚠️ 환각 감지, 자막 무시: {}", transcription);
                return;
            }

            Map<String, Object> subtitleMessage = new HashMap<>();
            subtitleMessage.put("sessionId", sessionId);
            subtitleMessage.put("userId", userId);
            subtitleMessage.put("userName", userName);
            subtitleMessage.put("text", transcription);
            subtitleMessage.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend(
                "/topic/session/" + sessionId + "/subtitle", 
                subtitleMessage
            );

            log.info("✅ 자막 전송 완료: {}", transcription);

        } catch (Exception e) {
            log.error("❌ 자막 생성 실패", e);
        }
    }

    private boolean isHallucination(String text) {
        if (text.length() < 2) {
            return true;
        }

        String lowerText = text.toLowerCase().replaceAll("\\s+", "");
        
        for (String pattern : HALLUCINATION_PATTERNS) {
            if (lowerText.contains(pattern.toLowerCase())) {
                log.info("🚫 환각 패턴 매칭: '{}' in '{}'", pattern, text);
                return true;
            }
        }

        long uniqueChars = text.chars().distinct().count();
        if (text.length() > 10 && uniqueChars < 3) {
            log.info("🚫 반복 문자 감지: {}", text);
            return true;
        }

        if (text.matches(".*[.。]\\s*$") && text.length() > 20) {
            log.info("🚫 문장 종결 형태 감지 (뉴스/방송): {}", text);
            return true;
        }

        String[] sentences = text.split("[.。!?]");
        if (sentences.length > 2) {
            log.info("🚫 복수 문장 감지 (환각 가능성 높음): {}", text);
            return true;
        }

        return false;
    }

    private String transcribeAudio(MultipartFile audio) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(openaiApiKey);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", audio.getResource());
            body.add("model", "whisper-1");
            body.add("language", "ko");
            body.add("temperature", "0.0");

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            log.info("📡 Whisper API 호출 (temperature=0.0)");
            
            ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.openai.com/v1/audio/transcriptions",
                HttpMethod.POST,
                requestEntity,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String result = (String) response.getBody().get("text");
                log.info("📥 Whisper 원본 결과: '{}'", result);
                return result;
            }

            log.warn("⚠️ Whisper API 응답 없음");
            return null;

        } catch (Exception e) {
            log.error("❌ Whisper API 호출 실패", e);
            return null;
        }
    }
}
