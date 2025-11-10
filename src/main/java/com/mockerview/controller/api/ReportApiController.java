package com.mockerview.controller.api;

import com.mockerview.entity.InterviewReport;
import com.mockerview.entity.User;
import com.mockerview.service.InterviewReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportApiController {

    private final InterviewReportService reportService;

    @PostMapping("/generate/{sessionId}")
    public ResponseEntity<?> generateReport(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal User user
    ) {
        try {
            reportService.generateReportAsync(sessionId);
            return ResponseEntity.ok(Map.of(
                    "message", "리포트 생성이 시작되었습니다",
                    "sessionId", sessionId
            ));
        } catch (Exception e) {
            log.error("리포트 생성 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> getReportBySession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal User user
    ) {
        try {
            InterviewReport report = reportService.getReportBySession(sessionId);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyReports(@AuthenticationPrincipal User user) {
        List<InterviewReport> reports = reportService.getUserReports(user.getId());
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/{reportId}/pdf")
    public ResponseEntity<?> getPDFUrl(
            @PathVariable Long reportId,
            @AuthenticationPrincipal User user
    ) {
        try {
            InterviewReport report = reportService.getReportBySession(reportId);
            return ResponseEntity.ok(Map.of("pdfUrl", report.getPdfUrl()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
