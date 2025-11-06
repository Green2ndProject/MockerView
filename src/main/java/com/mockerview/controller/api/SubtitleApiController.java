package com.mockerview.controller.api;

import com.mockerview.entity.User;
import com.mockerview.repository.UserRepository;
import com.mockerview.service.RealtimeSubtitleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/subtitle")
@RequiredArgsConstructor
public class SubtitleApiController {

    private final RealtimeSubtitleService subtitleService;
    private final UserRepository userRepository;

    @PostMapping("/{sessionId}/transcribe")
    public ResponseEntity<?> transcribeAudio(
            @PathVariable Long sessionId,
            @RequestParam("audio") MultipartFile audio,
            Authentication auth
    ) {
        try {
            String username = auth.getName();
            log.info("📥 자막 요청 - Session: {}, User: {}", sessionId, username);

            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            subtitleService.transcribeAndBroadcast(
                sessionId, 
                user.getId(), 
                user.getName(), 
                audio
            );

            return ResponseEntity.ok(Map.of("success", true));

        } catch (Exception e) {
            log.error("❌ 자막 처리 실패", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
