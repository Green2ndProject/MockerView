package com.mockerview.controller.web;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.dto.StatisticsDTO;
import com.mockerview.entity.SelfInterviewReport;
import com.mockerview.entity.Subscription;
import com.mockerview.entity.User;
import com.mockerview.repository.SelfInterviewReportRepository;
import com.mockerview.service.SubscriptionService;
import com.mockerview.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/auth/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final SelfInterviewReportRepository selfInterviewReportRepository;

    @GetMapping
    public String myPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        try {
            User user = userService.findByUsername(userDetails.getUsername());
            
            Subscription subscription = subscriptionService.getActiveSubscription(user.getId());
            
            List<SelfInterviewReport> selfReports = new ArrayList<>();
            long totalReports = 0;
            
            try {
                selfReports = selfInterviewReportRepository
                        .findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, 3));
                totalReports = selfInterviewReportRepository.countByUserId(user.getId());
            } catch (Exception e) {
                log.warn("셀프 리포트 조회 실패: {}", e.getMessage());
            }
            
            model.addAttribute("user", user);
            model.addAttribute("username", user.getUsername());
            model.addAttribute("email", user.getEmail());
            model.addAttribute("name", user.getName());
            model.addAttribute("createdAt", user.getCreatedAt());
            model.addAttribute("subscription", subscription);
            model.addAttribute("selfReports", selfReports);
            model.addAttribute("totalReports", totalReports);
            
            return "user/mypage";
        } catch (Exception e) {
            log.error("마이페이지 로딩 실패: {}", e.getMessage(), e);
            return "redirect:/";
        }
    }

    @PostMapping("/update")
    public String updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String name,
            @RequestParam String email,
            RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(userDetails.getUsername());
            user.setName(name);
            user.setEmail(email);
            userService.save(user);
            
            redirectAttributes.addFlashAttribute("successMessage", "프로필이 수정되었습니다.");
            return "redirect:/auth/mypage";
        } catch (Exception e) {
            log.error("프로필 수정 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "프로필 수정에 실패했습니다.");
            return "redirect:/auth/mypage";
        }
    }

    @GetMapping("/stats")
    public String myStats(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        try {
            User user = userService.findByUsername(userDetails.getUsername());
            
            if (user.getRole().name().contains("INTERVIEWER") || 
                user.getRole().name().contains("ADMIN") ||
                user.getRole().name().contains("HOST")) {
                return "redirect:/auth/mypage/myStatsInterviewer";
            }
            
            StatisticsDTO stats = userService.getUserStatistics(user.getId());
            
            List<Map<String, Object>> categoryAccuracyList = new ArrayList<>();
            for (var category : stats.getCategoryScores()) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", category.getCategory());
                item.put("accuracy", category.getAccuracy());
                categoryAccuracyList.add(item);
            }
            
            Map<String, Object> performanceChartData = new HashMap<>();
            performanceChartData.put("labels", new ArrayList<>(stats.getMonthlyProgress().keySet()));
            Map<String, Object> dataset = new HashMap<>();
            dataset.put("label", "면접 횟수");
            dataset.put("data", new ArrayList<>(stats.getMonthlyProgress().values()));
            dataset.put("borderColor", "#667eea");
            dataset.put("backgroundColor", "rgba(102, 126, 234, 0.1)");
            performanceChartData.put("datasets", List.of(dataset));
            
            Map<String, Object> activityChartData = new HashMap<>();
            activityChartData.put("labels", new ArrayList<>(stats.getMonthlyProgress().keySet()));
            Map<String, Object> activityDataset = new HashMap<>();
            activityDataset.put("label", "활동");
            activityDataset.put("data", new ArrayList<>(stats.getMonthlyProgress().values()));
            activityDataset.put("backgroundColor", "#667eea");
            activityChartData.put("datasets", List.of(activityDataset));
            
            model.addAttribute("user", user);
            model.addAttribute("totalInterviews", stats.getTotalSessions());
            model.addAttribute("averageScore", String.format("%.1f", stats.getAverageScore()));
            model.addAttribute("highestScore", String.format("%.1f", 
                stats.getCategoryScores().stream()
                    .mapToDouble(c -> c.getAccuracy())
                    .max()
                    .orElse(0.0)));
            model.addAttribute("totalAnswers", stats.getTotalAnswers());
            model.addAttribute("totalFeedbacks", stats.getTotalFeedbacks());
            model.addAttribute("mbtiType", stats.getMbtiType());
            model.addAttribute("completedSessions", stats.getCompletedSessions());
            model.addAttribute("categoryScores", stats.getCategoryScores());
            model.addAttribute("monthlyProgress", stats.getMonthlyProgress());
            model.addAttribute("categoryAccuracy", categoryAccuracyList);
            model.addAttribute("performanceChartData", performanceChartData);
            model.addAttribute("activityChartData", activityChartData);
            model.addAttribute("streak", "0일");
            model.addAttribute("achievements", new ArrayList<>());
            model.addAttribute("rankings", new ArrayList<>());
            model.addAttribute("achievementProgress", stats.getTotalAnswers() + "/100");
            model.addAttribute("interviewChange", null);
            model.addAttribute("scoreChange", null);
            model.addAttribute("highestScoreDate", null);
            model.addAttribute("streakStatus", null);
            
            log.info("✅ 통계 페이지 로딩 완료 - 총 세션: {}, 카테고리: {}", 
                stats.getTotalSessions(), categoryAccuracyList.size());
            
            return "user/myStats";
        } catch (Exception e) {
            log.error("내 통계 페이지 로딩 실패: {}", e.getMessage(), e);
            return "redirect:/auth/mypage";
        }
    }

    @GetMapping("/myStatsInterviewer")
    public String myStatsInterviewer(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        try {
            User user = userService.findByUsername(userDetails.getUsername());
            
            if (user.getRole().name().contains("USER") ||
                user.getRole().name().contains("STUDENT")) {
                return "redirect:/auth/mypage/stats";
            }
            
            Map<String, Object> stats = userService.getInterviewerStatistics(user.getId());
            
            model.addAttribute("user", user);
            model.addAttribute("totalHostedSessions", stats.get("totalHostedSessions"));
            model.addAttribute("endedSessionsCount", stats.get("endedSessionsCount"));
            model.addAttribute("totalFeedbacksGiven", stats.get("totalFeedbacksGiven"));
            model.addAttribute("avgGivenScore", "0.0");
            model.addAttribute("sessionsByMonth", stats.get("sessionsByMonth"));
            model.addAttribute("hostedSessions", new ArrayList<>());
            model.addAttribute("topInterviewees", new ArrayList<>());
            
            return "user/myStatsInterviewer";
        } catch (Exception e) {
            log.error("면접관 통계 페이지 로딩 실패: {}", e.getMessage(), e);
            return "redirect:/auth/mypage";
        }
    }

    @GetMapping("/withdraw")
    public String withdrawPage() {
        return "user/withdraw";
    }
}