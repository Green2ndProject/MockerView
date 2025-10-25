package com.mockerview.controller.api;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.Answer;
import com.mockerview.entity.Session;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.SessionRepository;
import com.mockerview.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/recording")
@RequiredArgsConstructor
public class RecordingApiController {

    private final CloudinaryService cloudinaryService;
    private final SessionRepository sessionRepository;
    private final AnswerRepository answerRepository;

    @PostMapping("/{sessionId}/upload")
    public ResponseEntity<?> uploadRecording(
            @PathVariable Long sessionId,
            @RequestParam("video") MultipartFile videoFile,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            if (userDetails == null) {
                log.error("❌ 인증 실패 - Authorization 헤더 없거나 잘못됨");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "로그인이 필요합니다"));
            }
            
            log.info("📹 녹화 업로드 요청 - sessionId: {}, userId: {}, 파일크기: {}MB",
                sessionId, userDetails.getUserId(), videoFile.getSize() / 1024.0 / 1024.0);

            Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다"));

            if (!session.getHost().getId().equals(userDetails.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "권한이 없습니다"));
            }

            String videoUrl = cloudinaryService.uploadVideo(videoFile, sessionId.toString());

            session.setVideoRecordingUrl(videoUrl);
            sessionRepository.save(session);

            log.info("✅ 녹화 업로드 완료 - sessionId: {}, URL: {}", sessionId, videoUrl);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "녹화가 업로드되었습니다",
                "videoUrl", videoUrl
            ));

        } catch (Exception e) {
            log.error("❌ 녹화 업로드 실패 - sessionId: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "업로드 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/{sessionId}/save-url")
    public ResponseEntity<?> saveRecordingUrl(
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            String videoUrl = request.get("videoUrl");
            log.info("📹 녹화 URL 저장 - sessionId: {}, URL: {}", sessionId, videoUrl);

            Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다"));

            if (!session.getHost().getId().equals(userDetails.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "권한이 없습니다"));
            }

            session.setVideoRecordingUrl(videoUrl);
            sessionRepository.save(session);

            log.info("✅ 녹화 URL 저장 완료 - sessionId: {}", sessionId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "녹화 URL이 저장되었습니다"
            ));

        } catch (Exception e) {
            log.error("❌ 녹화 URL 저장 실패 - sessionId: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "저장 실패: " + e.getMessage()));
        }
    }

    @GetMapping("/{sessionId}/video")
    public ResponseEntity<?> getRecording(@PathVariable Long sessionId) {
        try {
            Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다"));

            if (session.getVideoRecordingUrl() == null || session.getVideoRecordingUrl().isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "녹화가 없습니다"
                ));
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "videoUrl", session.getVideoRecordingUrl()
            ));

        } catch (Exception e) {
            log.error("❌ 녹화 조회 실패 - sessionId: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "조회 실패: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{sessionId}/video")
    public ResponseEntity<?> deleteRecording(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다"));

            if (!session.getHost().getId().equals(userDetails.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "권한이 없습니다"));
            }

            session.setVideoRecordingUrl(null);
            sessionRepository.save(session);

            log.info("✅ 녹화 삭제 완료 - sessionId: {}", sessionId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "녹화가 삭제되었습니다"
            ));

        } catch (Exception e) {
            log.error("❌ 녹화 삭제 실패 - sessionId: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "삭제 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/answer/{answerId}/upload")
    public ResponseEntity<?> uploadAnswerRecording(
            @PathVariable Long answerId,
            @RequestParam("video") MultipartFile videoFile,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            if (userDetails == null) {
                log.error("❌ 인증 실패 - Authorization 헤더 없거나 잘못됨");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "로그인이 필요합니다"));
            }
            
            if (videoFile == null || videoFile.isEmpty()) {
                log.error("❌ 비디오 파일이 없음 - answerId: {}", answerId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "비디오 파일이 없습니다"));
            }
            
            double fileSizeMB = videoFile.getSize() / 1024.0 / 1024.0;
            log.info("📹 답변 녹화 업로드 시작 - answerId: {}, userId: {}, 크기: {}MB, 파일명: {}",
                answerId, userDetails.getUserId(), fileSizeMB, videoFile.getOriginalFilename());

            Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("답변을 찾을 수 없습니다"));

            if (!answer.getUser().getId().equals(userDetails.getUserId())) {
                log.error("❌ 권한 없음 - answerId: {}, userId: {}, answer.userId: {}", 
                    answerId, userDetails.getUserId(), answer.getUser().getId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "본인의 답변만 업로드할 수 있습니다"));
            }

            log.info("⏳ Cloudinary 업로드 시작 - answerId: {}", answerId);
            String videoUrl = cloudinaryService.uploadVideo(videoFile, "answer_" + answerId);
            long uploadTime = System.currentTimeMillis() - startTime;
            log.info("⏱️ Cloudinary 업로드 완료 - 소요시간: {}ms", uploadTime);
            
            answer.setVideoUrl(videoUrl);
            answerRepository.save(answer);

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("✅ 답변 녹화 업로드 완료 - answerId: {}, URL: {}, 총 소요시간: {}ms", 
                answerId, videoUrl, totalTime);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "녹화가 업로드되었습니다",
                "videoUrl", videoUrl
            ));

        } catch (IOException e) {
            long failTime = System.currentTimeMillis() - startTime;
            log.error("❌ Cloudinary 업로드 실패 - answerId: {}, 소요시간: {}ms, 에러: {}", 
                answerId, failTime, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Cloudinary 업로드 실패: " + e.getMessage()));
                
        } catch (Exception e) {
            long failTime = System.currentTimeMillis() - startTime;
            log.error("❌ 답변 녹화 업로드 실패 - answerId: {}, 소요시간: {}ms", answerId, failTime, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "업로드 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/answer/{answerId}/save-url")
    public ResponseEntity<?> saveAnswerRecordingUrl(
            @PathVariable Long answerId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            String videoUrl = request.get("videoUrl");
            log.info("📹 답변 녹화 URL 저장 - answerId: {}, URL: {}", answerId, videoUrl);

            Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("답변을 찾을 수 없습니다"));

            if (!answer.getUser().getId().equals(userDetails.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "본인의 답변만 수정할 수 있습니다"));
            }

            answer.setVideoUrl(videoUrl);
            answerRepository.save(answer);

            log.info("✅ 답변 녹화 URL 저장 완료 - answerId: {}", answerId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "녹화 URL이 저장되었습니다"
            ));

        } catch (Exception e) {
            log.error("❌ 답변 녹화 URL 저장 실패 - answerId: {}", answerId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "저장 실패: " + e.getMessage()));
        }
    }

    @GetMapping("/answer/{answerId}/video")
    public ResponseEntity<?> getAnswerRecording(@PathVariable Long answerId) {
        try {
            Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("답변을 찾을 수 없습니다"));

            if (answer.getVideoUrl() == null || answer.getVideoUrl().isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "녹화가 없습니다"
                ));
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "videoUrl", answer.getVideoUrl()
            ));

        } catch (Exception e) {
            log.error("❌ 답변 녹화 조회 실패 - answerId: {}", answerId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "조회 실패: " + e.getMessage()));
        }
    }

    @DeleteMapping("/answer/{answerId}/video")
    public ResponseEntity<?> deleteAnswerRecording(
            @PathVariable Long answerId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("답변을 찾을 수 없습니다"));

            if (!answer.getUser().getId().equals(userDetails.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "본인의 답변만 삭제할 수 있습니다"));
            }

            answer.setVideoUrl(null);
            answerRepository.save(answer);

            log.info("✅ 답변 녹화 삭제 완료 - answerId: {}", answerId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "녹화가 삭제되었습니다"
            ));

        } catch (Exception e) {
            log.error("❌ 답변 녹화 삭제 실패 - answerId: {}", answerId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "삭제 실패: " + e.getMessage()));
        }
    }
}