package com.mockerview.controller.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.mockerview.entity.User;
import com.mockerview.entity.InterviewMBTI;
import com.mockerview.repository.UserRepository;
import com.mockerview.repository.InterviewMBTIRepository;
import com.mockerview.dto.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class AnalysisViewController {

    private final UserRepository userRepository;
    private final InterviewMBTIRepository interviewMBTIRepository;

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
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model
    ) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        InterviewMBTI mbtiData = interviewMBTIRepository.findLatestByUserId(currentUser.getId())
            .orElseThrow(() -> new RuntimeException("MBTI 분석 데이터가 없습니다"));
        
        model.addAttribute("mbti", mbtiData);
        model.addAttribute("userId", currentUser.getId());
        model.addAttribute("user", currentUser);
        
        return "analysis/mbti-detail";
    }
}
