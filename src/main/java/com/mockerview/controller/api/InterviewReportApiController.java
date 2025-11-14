package com.mockerview.controller.api;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.dto.InterviewReportDTO;
import com.mockerview.entity.InterviewReport;
import com.mockerview.service.InterviewReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class InterviewReportApiController {

    private final InterviewReportService reportService;

    @PostMapping("/generate/{sessionId}")
    public ResponseEntity<Map<String, Object>> generateReport(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다"));
            }

            Long userId = userDetails.getUserId();
            log.info("📊 리포트 생성 요청 - sessionId: {}, userId: {}", sessionId, userId);

            reportService.generateReportAsync(sessionId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "리포트 생성이 시작되었습니다");
            response.put("sessionId", sessionId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ 리포트 생성 요청 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<?> getReport(@PathVariable Long reportId) {
        try {
            InterviewReport report = reportService.getReportById(reportId);
            InterviewReportDTO dto = InterviewReportDTO.fromEntity(report);
            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            log.error("❌ 리포트 조회 실패 - reportId: {}", reportId, e);
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> getReportsBySession(@PathVariable Long sessionId) {
        try {
            List<InterviewReport> reports = reportService.getReportsBySessionId(sessionId);
            List<InterviewReportDTO> dtos = reports.stream()
                .map(InterviewReportDTO::fromEntity)
                .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            log.error("❌ 세션 리포트 목록 조회 실패 - sessionId: {}", sessionId, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/session/{sessionId}/latest")
    public ResponseEntity<?> getLatestReport(@PathVariable Long sessionId) {
        try {
            return reportService.getLatestCompletedReport(sessionId)
                .map(report -> ResponseEntity.ok(InterviewReportDTO.fromEntity(report)))
                .orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            log.error("❌ 최신 리포트 조회 실패 - sessionId: {}", sessionId, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{reportId}")
    public ResponseEntity<?> deleteReport(
            @PathVariable Long reportId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다"));
            }

            Long userId = userDetails.getUserId();
            reportService.deleteReport(reportId, userId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "리포트가 삭제되었습니다"
            ));

        } catch (Exception e) {
            log.error("❌ 리포트 삭제 실패 - reportId: {}", reportId, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/session/{sessionId}/status")
    public ResponseEntity<?> checkReportStatus(@PathVariable Long sessionId) {
        try {
            List<InterviewReport> reports = reportService.getReportsBySessionId(sessionId);
            
            boolean hasGenerating = reports.stream()
                .anyMatch(r -> r.getStatus() == InterviewReport.ReportStatus.GENERATING);
            
            boolean hasCompleted = reports.stream()
                .anyMatch(r -> r.getStatus() == InterviewReport.ReportStatus.COMPLETED);

            Map<String, Object> status = new HashMap<>();
            status.put("hasGenerating", hasGenerating);
            status.put("hasCompleted", hasCompleted);
            status.put("totalReports", reports.size());

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("❌ 리포트 상태 확인 실패 - sessionId: {}", sessionId, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
