package com.mockerview.controller.web;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.dto.RegisterDTO;
import com.mockerview.entity.Answer;
import com.mockerview.entity.Feedback;
import com.mockerview.entity.Session;
import com.mockerview.entity.User;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.SessionRepository;
import com.mockerview.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/auth")
@Slf4j
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private SessionRepository sessionRepository;
    
    @Autowired
    private AnswerRepository answerRepository;

    @GetMapping("/login")
    public String loginForm() {
        log.info("로그인폼 controller 진입 성공!");
        return "user/login";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "user/register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute RegisterDTO registerDTO,
                            RedirectAttributes redirectAttributes) {
        
        String username = registerDTO.getUsername();
        String password = registerDTO.getPassword();
        String name = registerDTO.getName();
        String email = registerDTO.getEmail();
        String role = registerDTO.getRole();

        if (userRepository.findByUsername(username).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "이미 존재하는 아이디입니다.");
            return "redirect:/auth/register";
        }
        
        User user = User.builder()
                        .username(username)
                        .password(passwordEncoder.encode(password))
                        .name(name)
                        .email(email)
                        .role(User.UserRole.valueOf(role))
                        .build();
        
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", "회원가입이 완료되었습니다.");
        return "redirect:/auth/login";
    }

    /**
     * ✅ 최적화: @AuthenticationPrincipal을 사용하여 DB 재조회 (username -> User) 과정을 ID 기반으로 변경
     */
    @GetMapping("/mypage")
    public String mypage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        
        // 비로그인 사용자 처리
        if (userDetails == null) {
            return "redirect:/auth/login";
        }
        
        // 💡 userDetails에서 ID를 바로 가져옵니다. (DB 재조회 최소화)
        Long userId = userDetails.getUserId();

        // 💡 ID를 사용하여 DB에서 User 엔티티를 조회합니다. (최적화된 DB 조회)
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
        
            try {
                // 이하는 ID를 사용한 로직은 그대로 유지합니다.
                List<Session> hostedSessions = sessionRepository.findByHostId(userId);
                List<Answer> userAnswers = answerRepository.findByUserId(userId);
                
                long participatedSessionCount = 0;
                long answerCount = 0;
                
                if (userAnswers != null && !userAnswers.isEmpty()) {
                    answerCount = userAnswers.size();
                    try {
                        participatedSessionCount = userAnswers.stream()
                            .filter(a -> a.getQuestion() != null && a.getQuestion().getSession() != null)
                            .map(a -> a.getQuestion().getSession().getId())
                            .distinct()
                            .count();
                    } catch (Exception e) {
                        log.error("세션 카운트 중 오류: ", e);
                        participatedSessionCount = 0;
                    }
                }
                
                model.addAttribute("user", user);
                model.addAttribute("hostedSessions", hostedSessions != null ? hostedSessions : List.of());
                model.addAttribute("participatedSessionCount", participatedSessionCount);
                model.addAttribute("answerCount", answerCount);
                
                return "user/mypage";
                
            } catch (Exception e) {
                log.error("마이페이지 로드 오류: ", e);
                model.addAttribute("error", "페이지를 불러올 수 없습니다.");
                return "redirect:/session/list";
            }
        }

        // userDetails는 있지만 DB에서 사용자를 찾지 못한 경우
        return "redirect:/auth/login";
    }

    /**
     * ✅ 최적화: @AuthenticationPrincipal을 사용하여 DB 재조회 (username -> User) 과정을 ID 기반으로 변경
     */
    @PostMapping("/mypage/update")
    public String updateProfile(@RequestParam String name,
                                @RequestParam String email,
                                @RequestParam(required = false) String password,
                                // 💡 @AuthenticationPrincipal로 DTO를 바로 받습니다.
                                @AuthenticationPrincipal CustomUserDetails userDetails, 
                                RedirectAttributes redirectAttributes) {
        
        if (userDetails == null) {
            return "redirect:/auth/login";
        }
        
        // 💡 userDetails에서 ID를 바로 가져옵니다.
        Long userId = userDetails.getUserId();
        
        // 💡 ID를 사용하여 DB에서 User 엔티티를 조회합니다.
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // ... (업데이트 로직은 동일)
            User updatedUser = user.toBuilder()
                    .name(name)
                    .email(email)
                    .build();
            
            if (password != null && !password.trim().isEmpty()) {
                updatedUser = updatedUser.toBuilder()
                        .password(passwordEncoder.encode(password))
                        .build();
            }
            
            userRepository.save(updatedUser);
            redirectAttributes.addFlashAttribute("success", "프로필이 업데이트되었습니다.");
        } else {
            // ID는 있지만 DB에서 사용자를 못 찾은 경우
            redirectAttributes.addFlashAttribute("error", "사용자 정보를 찾을 수 없습니다.");
        }
        
        return "redirect:/auth/mypage";
    }

    @GetMapping("/mypage/stats")
    public String showMyStats(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        try {
            log.info("Stats 페이지 진입");
            
            if (userDetails == null) {
                return "redirect:/auth/login";
            }
            
            Long userId = userDetails.getUserId();
            User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            List<Answer> myAnswers = answerRepository.findByUserIdWithFeedbacks(currentUser.getId());
            log.info("답변 {} 개 조회됨", myAnswers.size());
            
            List<Feedback> allFeedbacks = new ArrayList<>();
            for (Answer answer : myAnswers) {
                if (answer.getFeedbacks() != null) {
                    allFeedbacks.addAll(answer.getFeedbacks());
                }
            }
            
            long aiFeedbackCount = allFeedbacks.stream()
                .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.AI)
                .count();
            
            long interviewerFeedbackCount = allFeedbacks.stream()
                .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.INTERVIEWER)
                .count();
            
            Double avgAiScore = allFeedbacks.stream()
                .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.AI && f.getScore() != null)
                .mapToInt(Feedback::getScore)
                .average()
                .orElse(0.0);
            
            Double avgInterviewerScore = allFeedbacks.stream()
                .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.INTERVIEWER && f.getScore() != null)
                .mapToInt(Feedback::getScore)
                .average()
                .orElse(0.0);
            
            Map<String, Long> answersByMonth = new LinkedHashMap<>();
            for (Answer answer : myAnswers) {
                if (answer.getCreatedAt() != null) {
                    String month = answer.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                    answersByMonth.put(month, answersByMonth.getOrDefault(month, 0L) + 1);
                }
            }
            
            model.addAttribute("user", currentUser);
            model.addAttribute("totalAnswers", myAnswers.size());
            model.addAttribute("aiFeedbackCount", aiFeedbackCount);
            model.addAttribute("interviewerFeedbackCount", interviewerFeedbackCount);
            model.addAttribute("avgAiScore", String.format("%.1f", avgAiScore));
            model.addAttribute("avgInterviewerScore", String.format("%.1f", avgInterviewerScore));
            model.addAttribute("answersByMonth", answersByMonth);
            model.addAttribute("myAnswers", myAnswers);
            
            log.info("Stats 데이터 처리 완료");
            return "user/myStats";
            
        } catch (Exception e) {
            log.error("Stats 페이지 오류: ", e);
            model.addAttribute("error", "통계를 불러올 수 없습니다.");
            return "redirect:/auth/mypage";
        }
    }

    @GetMapping("/mypage/stats/export-csv")
    public void exportStatsCSV(@AuthenticationPrincipal CustomUserDetails userDetails, 
                                HttpServletResponse response) throws IOException {
        if (userDetails == null) {
            response.sendRedirect("/auth/login");
            return;
        }
        
        Long userId = userDetails.getUserId();
        User currentUser = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Answer> myAnswers = answerRepository.findByUserIdWithFeedbacks(currentUser.getId());
        
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"my_interview_stats.csv\"");
        
        response.getOutputStream().write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});
        
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
            
            writer.println("날짜,질문,답변,AI점수,면접관점수,AI피드백,면접관피드백");
            
            for (Answer answer : myAnswers) {
                String date = answer.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                String question = answer.getQuestion().getText().replace(",", " ");
                String answerText = answer.getText().replace(",", " ").replace("\n", " ");
                
                Integer aiScore = answer.getFeedbacks().stream()
                    .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.AI && f.getScore() != null)
                    .findFirst()
                    .map(Feedback::getScore)
                    .orElse(null);
                
                Integer interviewerScore = answer.getFeedbacks().stream()
                    .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.INTERVIEWER && f.getScore() != null)
                    .findFirst()
                    .map(Feedback::getScore)
                    .orElse(null);
                
                String aiFeedback = answer.getFeedbacks().stream()
                    .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.AI)
                    .findFirst()
                    .map(f -> (f.getSummary() != null ? f.getSummary() : "").replace(",", " ").replace("\n", " "))
                    .orElse("");
                
                String interviewerFeedback = answer.getFeedbacks().stream()
                    .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.INTERVIEWER)
                    .findFirst()
                    .map(f -> (f.getReviewerComment() != null ? f.getReviewerComment() : "").replace(",", " ").replace("\n", " "))
                    .orElse("");
                
                writer.println(String.format("%s,%s,%s,%s,%s,%s,%s",
                    date, question, answerText,
                    aiScore != null ? aiScore : "",
                    interviewerScore != null ? interviewerScore : "",
                    aiFeedback, interviewerFeedback
                ));
            }
        }
    }
}