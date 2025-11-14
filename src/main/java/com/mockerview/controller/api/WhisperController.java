package com.mockerview.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/whisper")
@RequiredArgsConstructor
public class WhisperController {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @PostMapping("/transcribe")
    public ResponseEntity<?> transcribe(@RequestParam("audio") MultipartFile audioFile) {
        try {
            if (audioFile.isEmpty() || audioFile.getSize() < 1024) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "text", "",
                    "message", "음성 파일이 너무 작습니다."
                ));
            }

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

            String text = (String) response.getBody().get("text");
            log.info("🎤 음성 인식 완료: {}", text);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "text", text
            ));

        } catch (Exception e) {
            log.error("음성 인식 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "text", "",
                "message", "음성 인식에 실패했습니다: " + e.getMessage()
            ));
        }
    }
}
