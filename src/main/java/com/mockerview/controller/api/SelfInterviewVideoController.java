package com.mockerview.controller.api;

import com.mockerview.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/self-interview")
@RequiredArgsConstructor
public class SelfInterviewVideoController {

    private final CloudinaryService cloudinaryService;

    @PostMapping("/upload-video")
    public ResponseEntity<Map<String, String>> uploadVideo(@RequestParam("video") MultipartFile video) {
        Map<String, String> response = new HashMap<>();
        
        try {
            log.info("📹 셀프면접 영상 업로드 시작 - 파일 크기: {} bytes", video.getSize());
            
            String sessionId = "selfinterview_" + System.currentTimeMillis();
            String videoUrl = cloudinaryService.uploadVideo(video, sessionId);
            
            log.info("✅ 셀프면접 영상 업로드 완료: {}", videoUrl);
            
            response.put("videoUrl", videoUrl);
            response.put("success", "true");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ 셀프면접 영상 업로드 실패", e);
            response.put("error", e.getMessage());
            response.put("success", "false");
            return ResponseEntity.status(500).body(response);
        }
    }
}
