package com.mockerview.controller.web;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.InterviewReport;
import com.mockerview.entity.Session;
import com.mockerview.repository.SessionRepository;
import com.mockerview.service.InterviewReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/auth/reports")
@RequiredArgsConstructor
public class InterviewReportViewController {

    private final InterviewReportService reportService;
    private final SessionRepository sessionRepository;

    @GetMapping("/session/{sessionId}")
    public String viewSessionReports(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        
        try {
            if (userDetails == null) {
                return "redirect:/auth/login";
            }

            Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다"));

            List<InterviewReport> reports = reportService.getReportsBySessionId(sessionId);

            model.addAttribute("session", session);
            model.addAttribute("reports", reports);
            model.addAttribute("hasReports", !reports.isEmpty());

            return "report/list";

        } catch (Exception e) {
            log.error("❌ 리포트 목록 페이지 로드 실패", e);
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    @GetMapping("/{reportId}")
    public String viewReport(
            @PathVariable Long reportId,
            Model model) {
        
        try {
            InterviewReport report = reportService.getReportById(reportId);

            model.addAttribute("report", report);
            model.addAttribute("session", report.getSession());

            return "report/detail";

        } catch (Exception e) {
            log.error("❌ 리포트 상세 페이지 로드 실패 - reportId: {}", reportId, e);
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }
}
