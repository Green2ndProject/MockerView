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

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
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
                .filter(a -> a.getQuestion() != null && a.getQuestion().getSession() != null)
                .map(answer -> answer.getQuestion().getSession().getId())
                .collect(Collectors.toSet());
            
            model.addAttribute("user", user);
            model.addAttribute("userAnswers", userAnswers);
            model.addAttribute("hostedSessions", hostedSessions);
            model.addAttribute("participatedSessionCount", participatedSessionIds.size());
            model.addAttribute("answerCount", userAnswers.size());
            
            log.info("마이페이지 로드 완료 - 답변: {}, 호스팅: {}", userAnswers.size(), hostedSessions.size());
            
            return "user/mypage";
            
        } catch (Exception e) {
            log.error("마이페이지 로드 실패", e);
            model.addAttribute("error", "마이페이지를 불러올 수 없습니다: " + e.getMessage());
            return "error/500";
        }
    }

    @GetMapping("/auth/mypage/stats")
    public String showMyStats(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        try {
            log.info("Stats 페이지 진입");
            
            if (userDetails == null) {
                return "redirect:/auth/login";
            }
            
            User currentUser = userRepository.findByUsername(userDetails.getUsername())
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

    @Transactional(readOnly = true)
    private String loadInterviewerStats(User currentUser, Model model) {
        try {
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
            
            List<Object[]> topInterviewees = new ArrayList<>();
            try {
                topInterviewees = answerRepository.findTopScoredInterviewees();
            } catch (Exception e) {
                log.warn("Top interviewees 조회 실패: {}", e.getMessage());
            }
            
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
        } catch (Exception e) {
            log.error("면접관 통계 로드 실패", e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    private String loadIntervieweeStats(User currentUser, Model model) {
        try {
            List<Answer> myAnswers = answerRepository.findByUserIdWithFeedbacks(currentUser.getId());
            if (myAnswers == null) {
                myAnswers = new ArrayList<>();
            }
            
            myAnswers.sort(Comparator.comparing(Answer::getCreatedAt).reversed());
            log.info("답변 {} 개 조회됨", myAnswers.size());
            
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
        } catch (Exception e) {
            log.error("면접자 통계 로드 실패", e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    private List<Map<String, Object>> calculateUserRankings(Long currentUserId) {
        try {
            List<Answer> allAnswers = answerRepository.findAll();
            
            Map<Long, List<Answer>> answersByUser = allAnswers.stream()
                .filter(a -> a.getUser() != null)
                .collect(Collectors.groupingBy(answer -> answer.getUser().getId()));
            
            List<Map<String, Object>> rankings = new ArrayList<>();
            
            for (Map.Entry<Long, List<Answer>> entry : answersByUser.entrySet()) {
                Long userId = entry.getKey();
                List<Answer> userAnswers = entry.getValue();
                
                try {
                    if (userAnswers.isEmpty()) continue;
                    
                    User user = userAnswers.get(0).getUser();
                    if (user == null || user.getName() == null) continue;
                    
                    String userName = user.getName();
                    
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
                    log.warn("Ranking 계산 중 오류 (userId: {}): {}", userId, e.getMessage());
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
        } catch (Exception e) {
            log.error("전체 랭킹 계산 실패", e);
            return new ArrayList<>();
        }
    }

    @GetMapping("/auth/mypage/stats/export-csv")
    public void exportStatsCSV(@AuthenticationPrincipal CustomUserDetails userDetails, 
                                HttpServletResponse response) throws IOException {
        try {
            if (userDetails == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "로그인이 필요합니다");
                return;
            }
            
            User currentUser = userRepository.findByUsername(userDetails.getUsername())
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
                    if (answer.getQuestion() == null) continue;
                    
                    String date = answer.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    String question = answer.getQuestion().getText() != null ? 
                        answer.getQuestion().getText().replace(",", " ").replace("\"", "'") : "";
                    String answerText = answer.getAnswerText() != null ?
                        answer.getAnswerText().replace(",", " ").replace("\n", " ").replace("\"", "'") : "";
                    
                    Integer aiScore = null;
                    Integer interviewerScore = null;
                    String aiFeedback = "";
                    String interviewerFeedback = "";
                    
                    if (answer.getFeedbacks() != null) {
                        aiScore = answer.getFeedbacks().stream()
                            .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.AI && f.getScore() != null)
                            .findFirst()
                            .map(Feedback::getScore)
                            .orElse(null);
                        
                        interviewerScore = answer.getFeedbacks().stream()
                            .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.INTERVIEWER && f.getScore() != null)
                            .findFirst()
                            .map(Feedback::getScore)
                            .orElse(null);
                        
                        aiFeedback = answer.getFeedbacks().stream()
                            .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.AI)
                            .findFirst()
                            .map(f -> (f.getSummary() != null ? f.getSummary() : "")
                                .replace(",", " ").replace("\n", " ").replace("\"", "'"))
                            .orElse("");
                        
                        interviewerFeedback = answer.getFeedbacks().stream()
                            .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.INTERVIEWER)
                            .findFirst()
                            .map(f -> (f.getReviewerComment() != null ? f.getReviewerComment() : "")
                                .replace(",", " ").replace("\n", " ").replace("\"", "'"))
                            .orElse("");
                    }
                    
                    writer.println(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                        date, question, answerText,
                        aiScore != null ? aiScore : "",
                        interviewerScore != null ? interviewerScore : "",
                        aiFeedback, interviewerFeedback
                    ));
                }
                
                writer.flush();
            }
        } catch (Exception e) {
            log.error("CSV 내보내기 실패", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "CSV 생성 실패: " + e.getMessage());
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