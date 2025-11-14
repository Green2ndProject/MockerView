package com.mockerview.controller.web;

import com.mockerview.dto.CategoryStatsDTO;
import com.mockerview.entity.SelfInterviewReport;
import com.mockerview.entity.User;
import com.mockerview.repository.SelfInterviewReportRepository;
import com.mockerview.repository.UserRepository;
import com.mockerview.service.MyPageStatsService;
import com.mockerview.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MyPageController {

    private final UserRepository userRepository;
    private final MyPageStatsService statsService;
    private final SubscriptionService subscriptionService;
    private final SelfInterviewReportRepository selfInterviewReportRepository;

    @GetMapping("/auth/mypage")
    public String myPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("user", user);
        model.addAttribute("hostedSessions", new ArrayList<>());
        model.addAttribute("participatedSessions", new ArrayList<>());
        model.addAttribute("recentReviews", new ArrayList<>());
        
        List<SelfInterviewReport> allReports = selfInterviewReportRepository.findByUserOrderByCreatedAtDesc(user);
        List<SelfInterviewReport> recentReports = allReports.stream()
                .limit(3)
                .collect(Collectors.toList());
        
        model.addAttribute("selfReports", recentReports);
        model.addAttribute("totalReports", allReports.size());
        
        var subscription = subscriptionService.getActiveSubscription(user.getId());
        model.addAttribute("subscription", subscription);

        return "user/mypage";
    }

    @GetMapping("/auth/mypage/stats")
    public String myPageStats(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("user", user);

        Map<String, Object> stats = statsService.getUserStats(user);
        model.addAttribute("totalSessions", stats.get("totalSessions"));
        model.addAttribute("totalAnswers", stats.get("totalAnswers"));
        model.addAttribute("avgScore", stats.get("avgScore"));
        model.addAttribute("totalFeedbacks", stats.get("totalFeedbacks"));
        model.addAttribute("growthRate", stats.get("growthRate"));
        
        @SuppressWarnings("unchecked")
        List<CategoryStatsDTO> categoryStats = (List<CategoryStatsDTO>) stats.get("categoryStats");
        model.addAttribute("categoryStats", categoryStats);

        @SuppressWarnings("unchecked")
        Map<String, Double> recentScores = (Map<String, Double>) stats.get("recentScores");
        model.addAttribute("recentScores", recentScores);

        var subscription = subscriptionService.getActiveSubscription(user.getId());
        model.addAttribute("subscription", subscription);

        return "user/myStats";
    }

    @GetMapping("/user/consent-management")
    public String consentManagement() {
        log.info("개인정보 동의 관리 페이지 접근");
        return "user/consent-management";
    }
}
