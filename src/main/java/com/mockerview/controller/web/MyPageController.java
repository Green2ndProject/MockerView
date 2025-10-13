package com.mockerview.controller.web;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.*;
import com.mockerview.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class MyPageController {

    private final UserRepository userRepository;
    private final AnswerRepository answerRepository;
    private final SessionRepository sessionRepository;
    private final FeedbackRepository feedbackRepository;
    private final PasswordEncoder passwordEncoder;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)) {
            return null;
        }
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userRepository.findByUsername(userDetails.getUsername()).orElse(null);
    }

    @GetMapping("/mypage")
    public String myPage(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }
        model.addAttribute("currentUser", currentUser);
        return "auth/mypage";
    }

    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public String showMyStats(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }
        
        log.info("통계 페이지 로드 - userId: {}, role: {}", currentUser.getId(), currentUser.getRole());
        
        if (currentUser.getRole() == User.UserRole.HOST) {
            return loadInterviewerStats(model, currentUser);
        } else {
            return loadIntervieweeStats(model, currentUser);
        }
    }

    private String loadInterviewerStats(Model model, User currentUser) {
        try {
            List<Session> hostedSessions = sessionRepository.findByHostId(currentUser.getId());
            
            int totalSessions = hostedSessions.size();
            int activeSessions = (int) hostedSessions.stream()
                .filter(s -> s.getSessionStatus() == Session.SessionStatus.RUNNING)
                .count();
            
            long totalAnswers = 0;
            for (Session session : hostedSessions) {
                totalAnswers += answerRepository.countByQuestionSessionId(session.getId());
            }
            
            List<Map<String, Object>> recentSessions = hostedSessions.stream()
                .sorted(Comparator.comparing(Session::getCreatedAt).reversed())
                .limit(5)
                .map(session -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", session.getId());
                    map.put("title", session.getTitle());
                    map.put("status", session.getSessionStatus());
                    map.put("createdAt", session.getCreatedAt());
                    map.put("answerCount", answerRepository.countByQuestionSessionId(session.getId()));
                    return map;
                })
                .collect(Collectors.toList());
            
            model.addAttribute("totalSessions", totalSessions);
            model.addAttribute("activeSessions", activeSessions);
            model.addAttribute("totalAnswers", totalAnswers);
            model.addAttribute("recentSessions", recentSessions);
            model.addAttribute("currentUser", currentUser);
            
            log.info("면접관 통계 로드 완료 - 세션: {}, 답변: {}", totalSessions, totalAnswers);
            
            return "auth/myStatsInterviewer";
            
        } catch (Exception e) {
            log.error("면접관 통계 로드 실패", e);
            model.addAttribute("error", "통계를 불러올 수 없습니다");
            return "redirect:/auth/mypage";
        }
    }

    private String loadIntervieweeStats(Model model, User currentUser) {
        try {
            List<Answer> answers = answerRepository.findByUserId(currentUser.getId());
            
            Map<Long, List<Feedback>> feedbackMap = new HashMap<>();
            for (Answer answer : answers) {
                List<Feedback> feedbacks = feedbackRepository.findByAnswerId(answer.getId());
                feedbackMap.put(answer.getId(), feedbacks);
                answer.setFeedbacks(feedbacks);
            }
            
            int totalAnswers = answers.size();
            
            double avgScore = answers.stream()
                .flatMap(a -> a.getFeedbacks().stream())
                .filter(f -> f.getScore() != null)
                .mapToInt(Feedback::getScore)
                .average()
                .orElse(0.0);
            
            Map<String, Double> categoryScores = new HashMap<>();
            categoryScores.put("기술", avgScore);
            
            List<Map<String, Object>> recentAnswers = answers.stream()
                .sorted(Comparator.comparing(Answer::getCreatedAt).reversed())
                .limit(5)
                .map(answer -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("question", answer.getQuestion().getText());
                    map.put("answerText", answer.getAnswerText());
                    map.put("createdAt", answer.getCreatedAt());
                    
                    Optional<Feedback> aiFeedback = answer.getFeedbacks().stream()
                        .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.AI)
                        .findFirst();
                    
                    map.put("score", aiFeedback.map(Feedback::getScore).orElse(null));
                    map.put("strengths", aiFeedback.map(Feedback::getStrengths).orElse(""));
                    map.put("improvements", aiFeedback.map(Feedback::getImprovementSuggestions).orElse(""));
                    
                    return map;
                })
                .collect(Collectors.toList());
            
            model.addAttribute("totalAnswers", totalAnswers);
            model.addAttribute("averageScore", Math.round(avgScore));
            model.addAttribute("categoryScores", categoryScores);
            model.addAttribute("recentAnswers", recentAnswers);
            model.addAttribute("currentUser", currentUser);
            
            log.info("면접자 통계 로드 완료 - 답변: {}, 평균: {}", totalAnswers, avgScore);
            
            return "auth/myStatsInterviewee";
            
        } catch (Exception e) {
            log.error("면접자 통계 로드 실패", e);
            model.addAttribute("error", "통계를 불러올 수 없습니다");
            return "redirect:/auth/mypage";
        }
    }

    @GetMapping("/edit")
    public String editProfile(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }
        model.addAttribute("currentUser", currentUser);
        return "auth/edit";
    }

    @PostMapping("/update")
    public String updateProfile(@RequestParam String name,
                                @RequestParam String email,
                                @RequestParam(required = false) String currentPassword,
                                @RequestParam(required = false) String newPassword,
                                RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        try {
            currentUser.setName(name);
            currentUser.setEmail(email);

            if (newPassword != null && !newPassword.isEmpty()) {
                if (currentPassword == null || currentPassword.isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", "현재 비밀번호를 입력해주세요");
                    return "redirect:/auth/edit";
                }
                
                if (!passwordEncoder.matches(currentPassword, currentUser.getPassword())) {
                    redirectAttributes.addFlashAttribute("error", "현재 비밀번호가 일치하지 않습니다");
                    return "redirect:/auth/edit";
                }
                
                currentUser.setPassword(passwordEncoder.encode(newPassword));
            }

            userRepository.save(currentUser);
            redirectAttributes.addFlashAttribute("success", "프로필이 수정되었습니다");
            
            log.info("프로필 수정 완료 - userId: {}", currentUser.getId());
            
            return "redirect:/auth/mypage";
            
        } catch (Exception e) {
            log.error("프로필 수정 실패", e);
            redirectAttributes.addFlashAttribute("error", "프로필 수정에 실패했습니다");
            return "redirect:/auth/edit";
        }
    }

    @PostMapping("/delete")
    public String deleteAccount(@RequestParam String password,
                                @RequestParam(required = false) String reason,
                                RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        if (!passwordEncoder.matches(password, currentUser.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "비밀번호가 일치하지 않습니다");
            return "redirect:/auth/mypage";
        }

        try {
            currentUser.setIsDeleted(1);
            currentUser.setDeletedAt(LocalDateTime.now());
            currentUser.setWithdrawalReason(reason);
            userRepository.save(currentUser);
            
            log.info("회원 탈퇴 완료 - userId: {}, reason: {}", currentUser.getId(), reason);
            
            SecurityContextHolder.clearContext();
            redirectAttributes.addFlashAttribute("success", "회원 탈퇴가 완료되었습니다");
            
            return "redirect:/auth/login";
            
        } catch (Exception e) {
            log.error("회원 탈퇴 실패", e);
            redirectAttributes.addFlashAttribute("error", "회원 탈퇴에 실패했습니다");
            return "redirect:/auth/mypage";
        }
    }
}