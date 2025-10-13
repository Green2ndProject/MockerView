package com.mockerview.controller.web;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.Answer;
import com.mockerview.entity.Session;
import com.mockerview.entity.User;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.SessionRepository;
import com.mockerview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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
@RequestMapping("/auth")
@RequiredArgsConstructor
public class MyPageController {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final AnswerRepository answerRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @GetMapping("/mypage")
    @Transactional(readOnly = true)
    public String showMyPage(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            log.info("===== 마이페이지 로드 시작 =====");
            log.info("사용자: {}, 역할: {}", currentUser.getName(), currentUser.getRole());
            
            List<Session> hostedSessions = sessionRepository.findByHostId(currentUser.getId());
            log.info("호스팅 세션: {} 개", hostedSessions.size());
            
            List<Answer> userAnswers = new ArrayList<>();
            try {
                userAnswers = answerRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
                
                for (Answer answer : userAnswers) {
                    if (answer.getQuestion() != null) {
                        answer.getQuestion().getText();
                        if (answer.getQuestion().getSession() != null) {
                            answer.getQuestion().getSession().getTitle();
                        }
                    }
                }
                
                log.info("답변: {} 개", userAnswers.size());
            } catch (Exception e) {
                log.error("❌ 답변 로드 실패: {}", e.getMessage());
                userAnswers = new ArrayList<>();
            }
            
            long participatedSessionCount = 0;
            long answerCount = 0;
            
            try {
                participatedSessionCount = answerRepository.countDistinctSessionsByUserId(currentUser.getId());
                answerCount = answerRepository.countByUserId(currentUser.getId());
            } catch (Exception e) {
                log.error("❌ 통계 로드 실패: {}", e.getMessage());
            }
            
            log.info("참가 세션: {}, 답변 수: {}", participatedSessionCount, answerCount);
            
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("hostedSessions", hostedSessions);
            model.addAttribute("userAnswers", userAnswers);
            model.addAttribute("participatedSessionCount", participatedSessionCount);
            model.addAttribute("answerCount", answerCount);
            
            return "user/mypage";
        } catch (Exception e) {
            log.error("❌❌❌ 마이페이지 완전 실패 ❌❌❌", e);
            model.addAttribute("error", "데이터 로드 실패: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/update")
    @Transactional
    public String updateProfile(@RequestParam String name,
                                @RequestParam String email,
                                @RequestParam(required = false) String newPassword,
                                @RequestParam(required = false) String currentPassword,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            user.setName(name);
            user.setEmail(email);
            
            if (newPassword != null && !newPassword.isEmpty()) {
                if (currentPassword == null || currentPassword.isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", "비밀번호 변경 시 현재 비밀번호를 입력해야 합니다.");
                    return "redirect:/auth/mypage";
                }
                
                if (!bCryptPasswordEncoder.matches(currentPassword, user.getPassword())) {
                    redirectAttributes.addFlashAttribute("error", "현재 비밀번호가 일치하지 않습니다.");
                    return "redirect:/auth/mypage";
                }
                
                user.setPassword(bCryptPasswordEncoder.encode(newPassword));
            }
            
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("success", "프로필이 업데이트되었습니다.");
            
        } catch (Exception e) {
            log.error("프로필 업데이트 실패", e);
            redirectAttributes.addFlashAttribute("error", "프로필 업데이트에 실패했습니다.");
        }
        
        return "redirect:/auth/mypage";
    }
}