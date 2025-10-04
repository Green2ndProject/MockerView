package com.mockerview.controller.web;

import com.mockerview.entity.User;
import com.mockerview.service.ReviewService;
import com.mockerview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {
    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);
    
    private final ReviewService reviewService;
    private final UserRepository userRepository;

    @GetMapping("/list")
    public String listPage(Model model) {
        try {
            log.info("리뷰 목록 페이지 접근");
            model.addAttribute("sessions", reviewService.getReviewableSessions());
            log.info("리뷰 목록 데이터 로드 완료");
            return "review/list";
        } catch (Exception e) {
            log.error("리뷰 목록 조회 실패", e);
            model.addAttribute("error", "리뷰 목록을 불러올 수 없습니다: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/detail/{sessionId}")
    public String detailPage(@PathVariable Long sessionId, Model model) {
        try {
            log.info("리뷰 상세 페이지 접근 - sessionId: {}", sessionId);
            model.addAttribute("sessionId", sessionId);
            return "review/detail";
        } catch (Exception e) {
            log.error("리뷰 상세 페이지 오류", e);
            model.addAttribute("error", "페이지를 불러올 수 없습니다: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/my")
    public String myReviewsPage(Authentication authentication, Model model) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            model.addAttribute("reviews", reviewService.getReviewsByReviewer(user.getId()));
            model.addAttribute("username", username);
            
            return "review/my";
        } catch (Exception e) {
            log.error("내 리뷰 목록 조회 오류", e);
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }
}