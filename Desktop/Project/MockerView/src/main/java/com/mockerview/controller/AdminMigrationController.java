package com.mockerview.controller;

import com.mockerview.service.SessionMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminMigrationController {

    private final SessionMigrationService migrationService;

    @GetMapping("/migration/preview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> previewMigration() {
        try {
            Map<String, Object> preview = migrationService.getMigrationPreview();
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            log.error("프리뷰 조회 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/migration/execute")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> executeMigration() {
        try {
            migrationService.migrateExistingSelfInterviews();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "셀프면접 세션 마이그레이션이 완료되었습니다."
            ));
        } catch (Exception e) {
            log.error("마이그레이션 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
}
