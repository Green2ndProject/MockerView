package com.mockerview.controller.web;

import com.mockerview.dto.CustomUserDetails;
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
import java.util.List;

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
            
            model.addAttribute("user", user);
            model.addAttribute("totalInterviews", 0);
            model.addAttribute("averageScore", "0.0");
            model.addAttribute("highestScore", "0");
            model.addAttribute("streak", "0일");
            model.addAttribute("categoryAccuracy", new ArrayList<>());
            model.addAttribute("achievements", new ArrayList<>());
            model.addAttribute("rankings", new ArrayList<>());
            model.addAttribute("achievementProgress", "0/10");
            
            return "user/myStats";
        } catch (Exception e) {
            log.error("내 통계 페이지 로딩 실패: {}", e.getMessage());
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
            
            model.addAttribute("user", user);
            model.addAttribute("totalHostedSessions", 0);
            model.addAttribute("endedSessionsCount", 0);
            model.addAttribute("totalFeedbacksGiven", 0);
            model.addAttribute("avgGivenScore", "0.0");
            model.addAttribute("sessionsByMonth", new java.util.HashMap<>());
            model.addAttribute("hostedSessions", new ArrayList<>());
            model.addAttribute("topInterviewees", new ArrayList<>());
            
            return "user/myStatsInterviewer";
        } catch (Exception e) {
            log.error("면접관 통계 페이지 로딩 실패: {}", e.getMessage());
            return "redirect:/auth/mypage";
        }
    }

    @GetMapping("/withdraw")
    public String withdrawPage() {
        return "user/withdraw";
    }
}
