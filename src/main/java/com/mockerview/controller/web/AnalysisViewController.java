package com.mockerview.controller.web;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.User;
import com.mockerview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/analysis")
@RequiredArgsConstructor
@Slf4j
public class AnalysisViewController {

    private final UserRepository userRepository;

    @GetMapping("/voice-result")
    public String voiceAnalysisPage(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model
    ) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        model.addAttribute("userId", currentUser.getId());
        model.addAttribute("user", currentUser);
        
        return "analysis/voice-result";
    }

    @GetMapping("/facial-result")
    public String facialAnalysisPage(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model
    ) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        model.addAttribute("userId", currentUser.getId());
        model.addAttribute("user", currentUser);
        
        return "analysis/facial-result";
    }

    @GetMapping("/mbti-result")
    public String mbtiAnalysisPage(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model
    ) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        model.addAttribute("userId", currentUser.getId());
        model.addAttribute("user", currentUser);
        
        return "analysis/mbti-result";
    }

    @GetMapping("/growth-comparison")
    public String growthComparisonPage(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model
    ) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        model.addAttribute("userId", currentUser.getId());
        model.addAttribute("user", currentUser);
        
        return "analysis/growth-comparison";
    }

    @GetMapping("/mbti/detail")
    public String mbtiDetailPage(
        @RequestParam String type,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model
    ) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        model.addAttribute("mbtiType", type);
        model.addAttribute("userId", currentUser.getId());
        model.addAttribute("user", currentUser);
        
        return "analysis/mbti-detail";
    }
}
