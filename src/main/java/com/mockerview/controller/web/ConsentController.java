package com.mockerview.controller.web;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.User;
import com.mockerview.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class ConsentController {

    private final UserService userService;

    @GetMapping("/consent-management")
    public String consentManagement(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        try {
            User user = userService.findByUsername(userDetails.getUsername());
            model.addAttribute("user", user);
            return "user/consent-management";
        } catch (Exception e) {
            log.error("개인정보 동의 관리 페이지 로딩 실패: {}", e.getMessage());
            return "redirect:/auth/mypage";
        }
    }
}
