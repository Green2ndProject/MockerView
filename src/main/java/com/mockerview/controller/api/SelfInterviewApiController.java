package com.mockerview.controller.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockerview.entity.SelfInterviewReport;
import com.mockerview.entity.User;
import com.mockerview.repository.SelfInterviewReportRepository;
import com.mockerview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/self-interview")
@RequiredArgsConstructor
@Slf4j
public class SelfInterviewApiController {

    private final SelfInterviewReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/save-report")
    public ResponseEntity<?> saveReport(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> reportData) {
        try {
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            SelfInterviewReport report = SelfInterviewReport.builder()
                    .user(user)
                    .categoryCode((String) reportData.get("categoryCode"))
                    .categoryName((String) reportData.get("categoryName"))
                    .questionType((String) reportData.get("questionType"))
                    .difficulty((Integer) reportData.get("difficulty"))
                    .totalQuestions((Integer) reportData.get("totalQuestions"))
                    .overallAvg(parseDouble(reportData.get("overallAvg")))
                    .textAvg(parseDouble(reportData.get("textAvg")))
                    .audioAvg(parseDouble(reportData.get("audioAvg")))
                    .videoAvg(parseDouble(reportData.get("videoAvg")))
                    .questionsData(objectMapper.writeValueAsString(reportData.get("questions")))
                    .feedbacksData(objectMapper.writeValueAsString(reportData.get("feedbacks")))
                    .build();

            reportRepository.save(report);

            log.info("셀프 면접 리포트 저장 완료 - userId: {}, reportId: {}", user.getId(), report.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "reportId", report.getId()
            ));

        } catch (JsonProcessingException e) {
            log.error("리포트 데이터 직렬화 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "리포트 저장에 실패했습니다."
            ));
        } catch (Exception e) {
            log.error("리포트 저장 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "리포트 저장에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/reports")
    public ResponseEntity<?> getReports(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<SelfInterviewReport> reports = reportRepository.findByUserOrderByCreatedAtDesc(user);

            return ResponseEntity.ok(reports);

        } catch (Exception e) {
            log.error("리포트 조회 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "리포트 조회에 실패했습니다."
            ));
        }
    }

    @GetMapping("/reports/{reportId}")
    public ResponseEntity<?> getReportDetail(@PathVariable Long reportId) {
        try {
            SelfInterviewReport report = reportRepository.findById(reportId)
                    .orElseThrow(() -> new RuntimeException("Report not found"));

            return ResponseEntity.ok(report);

        } catch (Exception e) {
            log.error("리포트 상세 조회 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "리포트 조회에 실패했습니다."
            ));
        }
    }

    private Double parseDouble(Object value) {
        if (value == null || value.equals("-")) return null;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
