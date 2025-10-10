package com.mockerview.controller.web;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.dto.RegisterDTO;
import com.mockerview.entity.Answer;
import com.mockerview.entity.Feedback;
import com.mockerview.entity.Session;
import com.mockerview.entity.User;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.FeedbackRepository;
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
import java.util.stream.Collectors;

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
    
    @Autowired
    private FeedbackRepository feedbackRepository;

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

    @GetMapping("/mypage")
    public String mypage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        
        if (userDetails == null) {
            return "redirect:/auth/login";
        }
        
        Long userId = userDetails.getUserId();
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
        
            try {
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
                model.addAttribute("userAnswers", userAnswers != null ? userAnswers : List.of());
                model.addAttribute("participatedSessionCount", participatedSessionCount);
                model.addAttribute("answerCount", answerCount);
                
                return "user/mypage";
                
            } catch (Exception e) {
                log.error("마이페이지 로드 오류: ", e);
                model.addAttribute("error", "페이지를 불러올 수 없습니다.");
                return "redirect:/session/list";
            }
        }

        return "redirect:/auth/login";
    }

    @PostMapping("/mypage/update")
    public String updateProfile(@RequestParam String name,
                                @RequestParam String email,
                                @RequestParam(required = false) String password,
                                @AuthenticationPrincipal CustomUserDetails userDetails, 
                                RedirectAttributes redirectAttributes) {
        
        if (userDetails == null) {
            return "redirect:/auth/login";
        }
        
        Long userId = userDetails.getUserId();
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
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
            
            model.addAttribute("user", currentUser);
            
            log.info("현재 사용자 Role: {}", currentUser.getRole());
            
            User.UserRole role = currentUser.getRole();
            if (role == User.UserRole.HOST || role == User.UserRole.REVIEWER) {
                log.info("면접관 통계 페이지로 이동");
                return loadInterviewerStats(currentUser, model);
            } else {
                log.info("면접자 통계 페이지로 이동");
                return loadIntervieweeStats(currentUser, model);
            }
            
        } catch (Exception e) {
            log.error("Stats 페이지 오류: ", e);
            model.addAttribute("error", "통계를 불러올 수 없습니다.");
            return "redirect:/auth/mypage";
        }
    }

    private String loadInterviewerStats(User currentUser, Model model) {
        List<Session> hostedSessions = sessionRepository.findByHostId(currentUser.getId());
        
        long totalHostedSessions = hostedSessions.size();
        
        long endedSessionsCount = hostedSessions.stream()
            .filter(s -> s.getStatus() == Session.SessionStatus.ENDED)
            .count();
        
        List<Feedback> givenFeedbacks = feedbackRepository.findByReviewerId(currentUser.getId());
        long totalFeedbacksGiven = givenFeedbacks.size();
        
        Double avgGivenScore = givenFeedbacks.stream()
            .filter(f -> f.getScore() != null)
            .mapToInt(Feedback::getScore)
            .average()
            .orElse(0.0);
        
        Map<String, Long> sessionsByMonth = new LinkedHashMap<>();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        for (Session session : hostedSessions) {
            if (session.getCreatedAt() != null) {
                String month = session.getCreatedAt().format(monthFormatter);
                sessionsByMonth.put(month, sessionsByMonth.getOrDefault(month, 0L) + 1);
            }
        }
        
        List<Object[]> topInterviewees = answerRepository.findTopScoredInterviewees();
        
        model.addAttribute("totalHostedSessions", totalHostedSessions);
        model.addAttribute("endedSessionsCount", endedSessionsCount);
        model.addAttribute("totalFeedbacksGiven", totalFeedbacksGiven);
        model.addAttribute("avgGivenScore", String.format("%.1f", avgGivenScore));
        model.addAttribute("sessionsByMonth", sessionsByMonth);
        model.addAttribute("hostedSessions", hostedSessions);
        model.addAttribute("givenFeedbacks", givenFeedbacks);
        model.addAttribute("topInterviewees", topInterviewees);
        
        log.info("면접관 통계 로드 완료");
        return "user/myStatsInterviewer";
    }

    private String loadIntervieweeStats(User currentUser, Model model) {
        List<Answer> myAnswers = answerRepository.findByUserIdWithFeedbacks(currentUser.getId());
        myAnswers.sort(Comparator.comparing(Answer::getCreatedAt).reversed());
        log.info("답변 {} 개 조회됨", myAnswers.size());
        
        for (Answer answer : myAnswers) {
            log.info("답변 ID: {}, 피드백 개수: {}", answer.getId(), 
                answer.getFeedbacks() != null ? answer.getFeedbacks().size() : 0);
            if (answer.getFeedbacks() != null) {
                for (Feedback f : answer.getFeedbacks()) {
                    log.info("  - 피드백 타입: {}, 점수: {}", f.getFeedbackType(), f.getScore());
                }
            }
        }
        
        List<Feedback> allFeedbacks = new ArrayList<>();
        for (Answer answer : myAnswers) {
            if (answer.getFeedbacks() != null) {
                allFeedbacks.addAll(answer.getFeedbacks());
            }
        }
        
        log.info("전체 피드백 개수: {}", allFeedbacks.size());
        
        long aiFeedbackCount = allFeedbacks.stream()
            .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.AI)
            .count();
        
        long interviewerFeedbackCount = allFeedbacks.stream()
            .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.INTERVIEWER)
            .count();
        
        log.info("AI 피드백: {}, 면접관 피드백: {}", aiFeedbackCount, interviewerFeedbackCount);
        
        
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
        
        List<Map<String, Object>> growthData = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");
        
        List<Answer> sortedAnswers = new ArrayList<>(myAnswers);
        sortedAnswers.sort(Comparator.comparing(Answer::getCreatedAt));
        
        for (Answer answer : sortedAnswers) {
            Map<String, Object> point = new HashMap<>();
            point.put("date", answer.getCreatedAt().format(formatter));
            
            OptionalDouble aiScoreOpt = OptionalDouble.empty();
            OptionalDouble interviewerScoreOpt = OptionalDouble.empty();
            
            if (answer.getFeedbacks() != null && !answer.getFeedbacks().isEmpty()) {
                aiScoreOpt = answer.getFeedbacks().stream()
                    .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.AI && f.getScore() != null)
                    .mapToInt(Feedback::getScore)
                    .average();
                
                interviewerScoreOpt = answer.getFeedbacks().stream()
                    .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.INTERVIEWER && f.getScore() != null)
                    .mapToInt(Feedback::getScore)
                    .average();
            }
            
            point.put("aiScore", aiScoreOpt.isPresent() ? aiScoreOpt.getAsDouble() : null);
            point.put("interviewerScore", interviewerScoreOpt.isPresent() ? interviewerScoreOpt.getAsDouble() : null);
            
            growthData.add(point);
        }
        
        List<Map<String, Object>> rankings = calculateUserRankings(currentUser.getId());
        
        model.addAttribute("totalAnswers", myAnswers.size());
        model.addAttribute("aiFeedbackCount", aiFeedbackCount);
        model.addAttribute("interviewerFeedbackCount", interviewerFeedbackCount);
        model.addAttribute("avgAiScore", String.format("%.1f", avgAiScore));
        model.addAttribute("avgInterviewerScore", String.format("%.1f", avgInterviewerScore));
        model.addAttribute("answersByMonth", answersByMonth);
        model.addAttribute("myAnswers", myAnswers);
        model.addAttribute("growthData", growthData);
        model.addAttribute("rankings", rankings);
        
        log.info("면접자 통계 로드 완료");
        return "user/myStats";
    }

    private List<Map<String, Object>> calculateUserRankings(Long currentUserId) {
        List<Answer> allAnswers = answerRepository.findAll();
        
        Map<Long, List<Answer>> answersByUser = allAnswers.stream()
            .collect(Collectors.groupingBy(answer -> answer.getUser().getId()));
        
        List<Map<String, Object>> rankings = new ArrayList<>();
        
        for (Map.Entry<Long, List<Answer>> entry : answersByUser.entrySet()) {
            Long userId = entry.getKey();
            List<Answer> userAnswers = entry.getValue();
            
            try {
                User user = userAnswers.get(0).getUser();
                
                if (user == null) {
                    log.warn("User is null for userId: {}, skipping", userId);
                    continue;
                }
                
                String userName;
                try {
                    userName = user.getName();
                    if (userName == null) {
                        log.warn("User name is null for userId: {}, skipping", userId);
                        continue;
                    }
                } catch (Exception e) {
                    log.warn("Failed to get user name for userId: {}, skipping. Error: {}", userId, e.getMessage());
                    continue;
                }
                
                List<Feedback> allFeedbacks = new ArrayList<>();
                for (Answer answer : userAnswers) {
                    if (answer.getFeedbacks() != null) {
                        allFeedbacks.addAll(answer.getFeedbacks());
                    }
                }
                
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
                
                double totalScore = 0.0;
                int scoreCount = 0;
                
                if (avgAiScore > 0) {
                    totalScore += avgAiScore;
                    scoreCount++;
                }
                if (avgInterviewerScore > 0) {
                    totalScore += avgInterviewerScore;
                    scoreCount++;
                }
                
                double finalAvgScore = scoreCount > 0 ? totalScore / scoreCount : 0.0;
                
                Map<String, Object> rankData = new HashMap<>();
                rankData.put("userId", userId);
                rankData.put("userName", userName);
                rankData.put("avgScore", Math.round(finalAvgScore * 10) / 10.0);
                rankData.put("answerCount", userAnswers.size());
                rankData.put("isCurrentUser", userId.equals(currentUserId));
                
                rankings.add(rankData);
                
            } catch (Exception e) {
                log.error("Error processing user ranking for userId: {}, skipping this user. Error: {}", userId, e.getMessage());
                continue;
            }
        }
        
        rankings.sort((a, b) -> Double.compare(
            (Double) b.get("avgScore"), 
            (Double) a.get("avgScore")
        ));
        
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).put("rank", i + 1);
        }
        
        return rankings;
    }
    
    @GetMapping("/mypage/stats/export-csv")
    public void exportStatsCSV(@AuthenticationPrincipal CustomUserDetails userDetails, 
                                HttpServletResponse response) throws IOException {
        if (userDetails == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
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
                String answerText = answer.getAnswerText().replace(",", " ").replace("\n", " ");
                
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

    @GetMapping("/withdraw")
    public String withdrawPage() {
        return "user/withdraw";
    }
}