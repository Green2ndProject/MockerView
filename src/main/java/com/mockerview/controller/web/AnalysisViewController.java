package com.mockerview.controller.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.mockerview.entity.User;
import com.mockerview.entity.InterviewMBTI;
import com.mockerview.repository.UserRepository;
import com.mockerview.repository.InterviewMBTIRepository;
import com.mockerview.dto.CustomUserDetails;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
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
        @RequestParam(required = false) String type,
        Model model
    ) {
        try {
            User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Optional<InterviewMBTI> mbtiOpt = interviewMBTIRepository.findLatestByUserId(currentUser.getId());
            
            if (mbtiOpt.isEmpty()) {
                log.info("MBTI 데이터 없음 - userId: {}, mbti-result로 리다이렉트", currentUser.getId());
                return "redirect:/analysis/mbti-result";
            }
            
            InterviewMBTI mbtiData = mbtiOpt.get();
            
            model.addAttribute("mbti", mbtiData);
            model.addAttribute("userId", currentUser.getId());
            model.addAttribute("user", currentUser);
            
            log.info("MBTI 상세 페이지 로드 - userId: {}, type: {}", currentUser.getId(), mbtiData.getMbtiType());
            
            return "analysis/mbti-detail";
            
        } catch (Exception e) {
            log.error("MBTI 상세 페이지 로드 실패: {}", e.getMessage(), e);
            return "redirect:/analysis/mbti-result";
        }
    }
}
