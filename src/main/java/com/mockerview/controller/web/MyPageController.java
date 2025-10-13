package com.mockerview.controller.web;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.Answer;
import com.mockerview.entity.Feedback;
import com.mockerview.entity.Session;
import com.mockerview.entity.User;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.FeedbackRepository;
import com.mockerview.repository.SessionRepository;
import com.mockerview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MyPageController {

    private final UserRepository userRepository;
    private final AnswerRepository answerRepository;
    private final SessionRepository sessionRepository;
    private final FeedbackRepository feedbackRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @GetMapping("/auth/mypage")
    @Transactional(readOnly = true)
    public String mypage(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            if (userDetails == null) {
                return "redirect:/auth/login";
            }
            
            User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            
            log.info("마이페이지 로드 - userId: {}, username: {}", user.getId(), user.getUsername());
            
            List<Answer> userAnswers = answerRepository.findByUserIdWithFeedbacks(user.getId());
            log.info("사용자 답변 수: {}", userAnswers.size());
            
            for (Answer answer : userAnswers) {
                if (answer.getQuestion() != null && answer.getQuestion().getSession() != null) {
                    answer.getQuestion().getSession().getTitle();
                }
            }
            
            List<Session> hostedSessions = sessionRepository.findByHostId(user.getId());
            log.info("호스팅한 세션 수: {}", hostedSessions.size());
            
            Set<Long> participatedSessionIds = userAnswers.stream()
                .map(answer -> answer.getQuestion().getSession().getId())
                .collect(Collectors.toSet());
            
            model.addAttribute("user", user);
            model.addAttribute("userAnswers", userAnswers);
            model.addAttribute("hostedSessions", hostedSessions);
            model.addAttribute("participatedSessionCount", participatedSessionIds.size());
            model.addAttribute("answerCount", userAnswers.size());
            
            log.info("마이페이지 로드 완료 - 답변: {}, 호스팅: {}", userAnswers.size(), hostedSessions.size());
            
            return "mypage/mypage";
            
        } catch (Exception e) {
            log.error("마이페이지 로드 실패", e);
            model.addAttribute("error", "마이페이지를 불러올 수 없습니다: " + e.getMessage());
            return "error/500";
        }
    }

    @GetMapping("/auth/mypage/stats")
    @Transactional(readOnly = true)
    public String userStats(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            if (userDetails == null) {
                return "redirect:/auth/login";
            }
            
            User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            
            List<Answer> myAnswers = answerRepository.findByUserIdWithFeedbacks(user.getId());
            
            long totalAnswers = myAnswers.size();
            
            long aiFeedbackCount = myAnswers.stream()
                .flatMap(a -> a.getFeedbacks().stream())
                .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.AI && f.getScore() != null)
                .count();
            
            long interviewerFeedbackCount = myAnswers.stream()
                .flatMap(a -> a.getFeedbacks().stream())
                .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.INTERVIEWER && f.getScore() != null)
                .count();
            
            double avgAiScore = myAnswers.stream()
                .flatMap(a -> a.getFeedbacks().stream())
                .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.AI && f.getScore() != null)
                .mapToInt(Feedback::getScore)
                .average()
                .orElse(0.0);
            
            double avgInterviewerScore = myAnswers.stream()
                .flatMap(a -> a.getFeedbacks().stream())
                .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.INTERVIEWER && f.getScore() != null)
                .mapToInt(Feedback::getScore)
                .average()
                .orElse(0.0);
            
            Map<String, Long> answersByMonth = myAnswers.stream()
                .collect(Collectors.groupingBy(
                    answer -> answer.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                    Collectors.counting()
                ));
            
            List<Map<String, Object>> growthData = myAnswers.stream()
                .sorted(Comparator.comparing(Answer::getCreatedAt))
                .map(answer -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("date", answer.getCreatedAt().format(DateTimeFormatter.ofPattern("MM-dd")));
                    
                    Double aiScore = answer.getFeedbacks().stream()
                        .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.AI && f.getScore() != null)
                        .mapToInt(Feedback::getScore)
                        .average()
                        .orElse(Double.NaN);
                    
                    Double interviewerScore = answer.getFeedbacks().stream()
                        .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.INTERVIEWER && f.getScore() != null)
                        .mapToInt(Feedback::getScore)
                        .average()
                        .orElse(Double.NaN);
                    
                    data.put("aiScore", Double.isNaN(aiScore) ? null : aiScore);
                    data.put("interviewerScore", Double.isNaN(interviewerScore) ? null : interviewerScore);
                    
                    return data;
                })
                .collect(Collectors.toList());
            
            List<Object[]> allScores = answerRepository.findAllUserAverageScores();
            List<Map<String, Object>> rankings = new ArrayList<>();
            int rank = 1;
            for (Object[] row : allScores) {
                Map<String, Object> rankData = new HashMap<>();
                rankData.put("rank", rank++);
                rankData.put("userId", row[0]);
                rankData.put("userName", row[1]);
                rankData.put("avgScore", Math.round((Double) row[2] * 10) / 10.0);
                rankData.put("answerCount", row[3]);
                rankData.put("isCurrentUser", row[0].equals(user.getId()));
                rankings.add(rankData);
            }
            
            model.addAttribute("user", user);
            model.addAttribute("totalAnswers", totalAnswers);
            model.addAttribute("aiFeedbackCount", aiFeedbackCount);
            model.addAttribute("interviewerFeedbackCount", interviewerFeedbackCount);
            model.addAttribute("avgAiScore", Math.round(avgAiScore * 10) / 10.0);
            model.addAttribute("avgInterviewerScore", Math.round(avgInterviewerScore * 10) / 10.0);
            model.addAttribute("answersByMonth", answersByMonth);
            model.addAttribute("myAnswers", myAnswers);
            model.addAttribute("growthData", growthData);
            model.addAttribute("rankings", rankings);
            
            return "mypage/stats";
            
        } catch (Exception e) {
            log.error("활동 통계 로드 실패", e);
            model.addAttribute("error", "통계를 불러올 수 없습니다: " + e.getMessage());
            return "error/500";
        }
    }

    @PostMapping("/auth/mypage/update")
    @Transactional
    public String updateProfile(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam(required = false) String password,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        
        try {
            User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            
            user.setName(name);
            user.setEmail(email);
            
            if (password != null && !password.isEmpty()) {
                user.setPassword(passwordEncoder.encode(password));
            }
            
            userRepository.save(user);
            
            redirectAttributes.addFlashAttribute("success", "프로필이 업데이트되었습니다");
            return "redirect:/auth/mypage";
            
        } catch (Exception e) {
            log.error("프로필 업데이트 실패", e);
            redirectAttributes.addFlashAttribute("error", "프로필 업데이트에 실패했습니다: " + e.getMessage());
            return "redirect:/auth/mypage";
        }
    }
}
