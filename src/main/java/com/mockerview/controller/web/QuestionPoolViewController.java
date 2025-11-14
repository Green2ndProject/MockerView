package com.mockerview.controller.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/admin/questionpool")
@RequiredArgsConstructor
public class QuestionPoolViewController {

    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public String showAnalyticsDashboard() {
        log.info("📊 QuestionPool 분석 대시보드 접근");
        return "admin/questionpool-analytics";
    }
}
