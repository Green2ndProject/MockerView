package com.mockerview.controller.web;

import com.mockerview.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @GetMapping("/list")
    public String listPage(Model model) {
        model.addAttribute("sessions", reviewService.getReviewableSessions());
        return "review/list";
    }

    @GetMapping("/detail/{sessionId}")
    public String detailPage(@PathVariable Long sessionId, Model model) {
        model.addAttribute("sessionId", sessionId);
        return "review/detail";
    }
}
