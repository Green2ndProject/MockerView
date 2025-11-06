package com.mockerview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockerview.dto.AnalysisCompleteMessage;
import com.mockerview.dto.VoiceAnalysisDTO;
import com.mockerview.entity.Answer;
import com.mockerview.entity.VoiceAnalysis;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.VoiceAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdvancedVoiceAnalysisService {

    private final VoiceAnalysisRepository voiceAnalysisRepository;
    private final AnswerRepository answerRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    
    @Value("${openai.api.key}")
    private String openaiApiKey;

    private static final List<String> KOREAN_FILLER_WORDS = Arrays.asList(
        "음", "어", "그", "저", "뭐", "좀", "약간", "이제", "그냥", 
        "막", "되게", "진짜", "아", "네", "예"
    );

    @Async
    @Transactional
    public void analyzeVoiceAsync(Long answerId, MultipartFile audioFile, String transcription) {
        try {
            log.info("🎤 고급 음성 분석 시작 - answerId: {}", answerId);
            
            Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Answer not found"));

            int fillerCount = countFillerWords(transcription);
            double speakingSpeed = calculateSpeakingSpeed(transcription, audioFile);
            Map<String, Object> pauseAnalysis = analyzePauses(transcription);
            
            String gptAnalysis = analyzeWithGPT(transcription, fillerCount, speakingSpeed);
            
            VoiceAnalysis voiceAnalysis = VoiceAnalysis.builder()
                .answer(answer)
                .speakingSpeed(speakingSpeed)
                .fillerWordsCount(fillerCount)
                .pauseCount((Integer) pauseAnalysis.get("count"))
                .avgPauseDuration((Double) pauseAnalysis.get("avgDuration"))
                .voiceStability(calculateVoiceStability(speakingSpeed, fillerCount))
                .pronunciationScore(calculatePronunciationScore(transcription))
                .fillerWordsDetail(extractFillerWordsDetail(transcription))
                .improvementSuggestions(gptAnalysis)
                .build();
            
            voiceAnalysisRepository.save(voiceAnalysis);
            
            log.info("✅ 음성 분석 완료 - 속도: {}, 필러워드: {}개", speakingSpeed, fillerCount);
            
            AnalysisCompleteMessage message = AnalysisCompleteMessage.builder()
                .answerId(answerId)
                .userId(answer.getUser().getId())
                .analysisType("VOICE")
                .message("음성 분석이 완료되었습니다")
                .data(VoiceAnalysisDTO.from(voiceAnalysis))
                .build();
            
            messagingTemplate.convertAndSend(
                "/topic/user/" + answer.getUser().getId() + "/analysis",
                message
            );
            
            notificationService.sendVoiceAnalysisComplete(answer.getUser().getId(), answerId);
            
        } catch (Exception e) {
            log.error("❌ 음성 분석 실패", e);
        }
    }

    private int countFillerWords(String text) {
        int count = 0;
        for (String filler : KOREAN_FILLER_WORDS) {
            Pattern pattern = Pattern.compile("\\b" + filler + "\\b");
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                count++;
            }
        }
        return count;
    }

    private double calculateSpeakingSpeed(String text, MultipartFile audioFile) {
        try {
            if (text == null || text.trim().isEmpty()) {
                log.warn("텍스트가 비어있어 기본 속도 반환");
                return 120.0;
            }

            String[] words = text.trim().split("\\s+");
            int wordCount = words.length;
            
            if (wordCount == 0) {
                log.warn("단어 수가 0, 기본 속도 반환");
                return 120.0;
            }

            long fileSizeInBytes = audioFile.getSize();
            if (fileSizeInBytes == 0) {
                log.warn("파일 크기가 0, 단어 기반 추정");
                return wordCount * 0.4;
            }

            double durationInSeconds = Math.max(1.0, fileSizeInBytes / 16000.0);
            double wordsPerMinute = (wordCount / durationInSeconds) * 60.0;

            if (Double.isNaN(wordsPerMinute) || Double.isInfinite(wordsPerMinute)) {
                log.warn("계산 결과 NaN/Infinity, 기본값 반환");
                return 120.0;
            }

            wordsPerMinute = Math.max(30.0, Math.min(300.0, wordsPerMinute));
            
            return Math.round(wordsPerMinute * 10.0) / 10.0;
            
        } catch (Exception e) {
            log.error("말 속도 계산 실패, 기본값 사용", e);
            return 120.0;
        }
    }

    private Map<String, Object> analyzePauses(String text) {
        Map<String, Object> result = new HashMap<>();
        
        if (text == null || text.trim().isEmpty()) {
            result.put("count", 0);
            result.put("avgDuration", 0.0);
            return result;
        }

        String[] sentences = text.split("[.!?]");
        int pauseCount = Math.max(0, sentences.length - 1);
        
        double avgDuration = 0.0;
        if (pauseCount > 0) {
            int totalLength = text.length();
            int avgSentenceLength = totalLength / sentences.length;
            avgDuration = Math.max(0.5, Math.min(3.0, avgSentenceLength / 30.0));
        }
        
        avgDuration = Math.round(avgDuration * 10.0) / 10.0;
        
        result.put("count", pauseCount);
        result.put("avgDuration", avgDuration);
        
        return result;
    }

    private int calculateVoiceStability(double speed, int fillerCount) {
        int score = 100;
        
        if (Double.isNaN(speed) || Double.isInfinite(speed)) {
            return 70;
        }

        if (speed < 80) {
            score -= 25;
        } else if (speed < 100) {
            score -= 15;
        } else if (speed > 200) {
            score -= 20;
        } else if (speed > 160) {
            score -= 10;
        }

        score -= Math.min(30, fillerCount * 2);

        return Math.max(0, Math.min(100, score));
    }

    private int calculatePronunciationScore(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 50;
        }

        int score = 85;
        
        if (text.contains("ㅋㅋ") || text.contains("ㅎㅎ") || text.contains("...")) {
            score -= 10;
        }
        
        if (text.length() < 30) {
            score -= 15;
        } else if (text.length() < 50) {
            score -= 10;
        }

        int koreanCount = 0;
        for (char c : text.toCharArray()) {
            if (c >= 0xAC00 && c <= 0xD7A3) {
                koreanCount++;
            }
        }
        
        if (koreanCount > text.length() * 0.7) {
            score += 10;
        }

        long sentenceCount = text.chars().filter(ch -> ch == '.' || ch == '!' || ch == '?').count();
        if (sentenceCount >= 3) {
            score += 5;
        }

        return Math.max(0, Math.min(100, score));
    }

    private String extractFillerWordsDetail(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "필러워드 사용 없음";
        }

        Map<String, Integer> fillerCounts = new HashMap<>();
        
        for (String filler : KOREAN_FILLER_WORDS) {
            Pattern pattern = Pattern.compile("\\b" + filler + "\\b");
            Matcher matcher = pattern.matcher(text);
            int count = 0;
            while (matcher.find()) {
                count++;
            }
            if (count > 0) {
                fillerCounts.put(filler, count);
            }
        }
        
        if (fillerCounts.isEmpty()) {
            return "✅ 필러워드 사용 없음 (매우 우수)";
        }
        
        StringBuilder detail = new StringBuilder("사용된 필러워드: ");
        fillerCounts.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(5)
            .forEach(entry -> detail.append(String.format("'%s'(%d회) ", entry.getKey(), entry.getValue())));
        
        return detail.toString().trim();
    }

    private String analyzeWithGPT(String transcription, int fillerCount, double speed) {
        try {
            if (transcription == null || transcription.trim().isEmpty()) {
                return "음성 분석 권장사항:\n• 명확한 발음으로 말하기\n• 적절한 말 속도 유지하기\n• 필러워드 줄이기";
            }

            String speedEval;
            if (speed < 100) {
                speedEval = "느림 (100 이하)";
            } else if (speed < 140) {
                speedEval = "적정 (100-140)";
            } else if (speed < 180) {
                speedEval = "빠름 (140-180)";
            } else {
                speedEval = "매우 빠름 (180 이상)";
            }

            String prompt = String.format(
                "다음은 면접 답변의 음성 분석 결과입니다:\n\n" +
                "발화 내용 (처음 200자): %s\n" +
                "말 속도: %.1f 단어/분 (%s)\n" +
                "필러워드 사용: %d회\n\n" +
                "음성 전달력 관점에서 구체적이고 실천 가능한 3가지 개선 제안을 번호를 매겨 간결하게 제시해주세요.",
                transcription.substring(0, Math.min(200, transcription.length())),
                speed,
                speedEval,
                fillerCount
            );

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o-mini");
            requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "당신은 음성 커뮤니케이션 및 스피치 전문가입니다. 간결하고 구체적인 피드백을 제공합니다."),
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("max_tokens", 400);
            requestBody.put("temperature", 0.7);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + openaiApiKey);
            headers.set("Content-Type", "application/json");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                "https://api.openai.com/v1/chat/completions",
                entity,
                String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            String analysis = root.path("choices").get(0).path("message").path("content").asText();
            
            if (analysis == null || analysis.trim().isEmpty()) {
                return getDefaultSuggestions(speed, fillerCount);
            }
            
            return "🎯 AI 음성 분석 피드백\n\n" + analysis;

        } catch (Exception e) {
            log.error("GPT 분석 실패", e);
            return getDefaultSuggestions(speed, fillerCount);
        }
    }

    private String getDefaultSuggestions(double speed, int fillerCount) {
        StringBuilder suggestions = new StringBuilder("💡 음성 개선 제안\n\n");
        
        if (speed < 100) {
            suggestions.append("1. 말 속도 향상\n");
            suggestions.append("   • 현재 속도가 느립니다 (100 이하)\n");
            suggestions.append("   • 더 활기차게 말해보세요\n\n");
        } else if (speed > 180) {
            suggestions.append("1. 말 속도 조절\n");
            suggestions.append("   • 현재 속도가 빠릅니다 (180 이상)\n");
            suggestions.append("   • 천천히, 명확하게 발음하세요\n\n");
        } else {
            suggestions.append("1. 말 속도 유지\n");
            suggestions.append("   • 적절한 속도를 유지하고 있습니다\n\n");
        }
        
        if (fillerCount > 10) {
            suggestions.append("2. 필러워드 줄이기\n");
            suggestions.append(String.format("   • 불필요한 추임새가 %d회 사용되었습니다\n", fillerCount));
            suggestions.append("   • '음', '어' 대신 잠깐 멈추기 연습하세요\n\n");
        } else if (fillerCount > 5) {
            suggestions.append("2. 필러워드 관리\n");
            suggestions.append(String.format("   • 추임새가 %d회 사용되었습니다\n", fillerCount));
            suggestions.append("   • 조금만 더 줄여보세요\n\n");
        } else {
            suggestions.append("2. 필러워드 관리 우수\n");
            suggestions.append("   • 깔끔한 발화를 유지하고 있습니다\n\n");
        }
        
        suggestions.append("3. 명확한 발음\n");
        suggestions.append("   • 자음과 모음을 정확하게 발음하세요\n");
        suggestions.append("   • 문장 끝을 흐리지 말고 명확히 마무리하세요\n");
        
        return suggestions.toString();
    }
}