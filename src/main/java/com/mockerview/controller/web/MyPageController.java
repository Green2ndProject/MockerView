package com.mockerview.controller.web;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.*;
import com.mockerview.repository.*;
import com.mockerview.service.SubscriptionService;
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
    private final VoiceAnalysisRepository voiceAnalysisRepository;
    private final FacialAnalysisRepository facialAnalysisRepository;
    private final InterviewMBTIRepository interviewMBTIRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SubscriptionService subscriptionService;

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
            
            com.mockerview.entity.Subscription subscription = subscriptionService.getActiveSubscription(user.getId());
            
            if (subscription != null) {
                model.addAttribute("planType", subscription.getPlanType().name());
                model.addAttribute("usedSessions", subscription.getUsedSessions());
                model.addAttribute("sessionLimit", subscription.getSessionLimit());
                model.addAttribute("usedReviewReads", subscription.getUsedReviewReads() != null ? subscription.getUsedReviewReads() : 0);
                model.addAttribute("reviewReadLimit", subscription.getReviewReadLimit() != null ? subscription.getReviewReadLimit() : 3);
                model.addAttribute("startDate", subscription.getStartDate());
                model.addAttribute("endDate", subscription.getEndDate());
            } else {
                model.addAttribute("planType", "FREE");
                model.addAttribute("usedSessions", 0);
                model.addAttribute("sessionLimit", 5);
                model.addAttribute("usedReviewReads", 0);
                model.addAttribute("reviewReadLimit", 3);
                model.addAttribute("startDate", null);
                model.addAttribute("endDate", null);
            }
            
            List<Answer> userAnswers = answerRepository.findByUserIdWithFeedbacks(user.getId());
            log.info("사용자 답변 수: {}", userAnswers.size());
            
            List<Answer> answersWithVideo = userAnswers.stream()
                .filter(a -> a.getVideoUrl() != null && !a.getVideoUrl().isEmpty())
                .collect(Collectors.toList());
            log.info("녹화가 있는 답변 수: {}", answersWithVideo.size());
            
            for (Answer answer : userAnswers) {
                if (answer.getQuestion() != null && answer.getQuestion().getSession() != null) {
                    answer.getQuestion().getSession().getTitle();
                }
            }
            
            List<Session> hostedSessions = sessionRepository.findByHostId(user.getId());
            log.info("호스팅한 세션 수: {}", hostedSessions.size());
            
            List<Session> sessionsWithRecording = sessionRepository.findByHostIdWithRecording(user.getId());
            log.info("녹화가 있는 세션 수: {}", sessionsWithRecording.size());
            
            Set<Long> participatedSessionIds = userAnswers.stream()
                .filter(a -> a.getQuestion() != null && a.getQuestion().getSession() != null)
                .map(answer -> answer.getQuestion().getSession().getId())
                .collect(Collectors.toSet());
            
            model.addAttribute("user", user);
            model.addAttribute("userAnswers", userAnswers);
            model.addAttribute("answersWithVideo", answersWithVideo);
            model.addAttribute("sessionsWithRecording", sessionsWithRecording);
            model.addAttribute("hostedSessions", hostedSessions);
            model.addAttribute("participatedSessionCount", participatedSessionIds.size());
            model.addAttribute("answerCount", userAnswers.size());
            
            log.info("마이페이지 로드 완료 - 답변: {}, 호스팅: {}, 녹화: {}, 세션녹화: {}", 
                userAnswers.size(), hostedSessions.size(), answersWithVideo.size(), sessionsWithRecording.size());
            
            return "user/mypage";
            
        } catch (Exception e) {
            log.error("마이페이지 로드 실패", e);
            model.addAttribute("error", "마이페이지를 불러올 수 없습니다: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/auth/mypage/stats")
    @Transactional(readOnly = true)
    public String showMyStats(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        try {
            if (userDetails == null) {
                return "redirect:/auth/login";
            }
            
            User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            model.addAttribute("user", currentUser);
            
            User.UserRole role = currentUser.getRole();
            if (role == User.UserRole.HOST || role == User.UserRole.REVIEWER) {
                return loadInterviewerStats(currentUser, model);
            } else {
                return loadIntervieweeStats(currentUser, model);
            }
            
        } catch (Exception e) {
            log.error("Stats 페이지 오류: ", e);
            model.addAttribute("error", "통계를 불러올 수 없습니다.");
            return "redirect:/auth/mypage";
        }
    }

    private String loadInterviewerStats(User currentUser, Model model) {
        try {
            List<Session> hostedSessions = sessionRepository.findByHostId(currentUser.getId());
            List<Feedback> givenFeedbacks = feedbackRepository.findByReviewerId(currentUser.getId());
            
            long totalHostedSessions = hostedSessions.size();
            long endedSessionsCount = hostedSessions.stream()
                .filter(s -> s.getStatus() == Session.SessionStatus.ENDED)
                .count();
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
                topInterviewees = answerRepository.findAllUserAverageScores();
                if (topInterviewees.size() > 10) {
                    topInterviewees = topInterviewees.subList(0, 10);
                }
            } catch (Exception e) {
                log.warn("상위 면접자 조회 실패", e);
            }
            
            model.addAttribute("totalHostedSessions", totalHostedSessions);
            model.addAttribute("endedSessionsCount", endedSessionsCount);
            model.addAttribute("totalFeedbacksGiven", totalFeedbacksGiven);
            model.addAttribute("avgGivenScore", Math.round(avgGivenScore * 10) / 10.0);
            model.addAttribute("sessionsByMonth", sessionsByMonth);
            model.addAttribute("topInterviewees", topInterviewees);
            model.addAttribute("hostedSessions", hostedSessions);
            model.addAttribute("givenFeedbacks", givenFeedbacks);
            
            return "user/myStatsInterviewer";
            
        } catch (Exception e) {
            log.error("면접관 통계 로드 실패", e);
            model.addAttribute("error", "통계를 불러올 수 없습니다.");
            return "redirect:/auth/mypage";
        }
    }

    private String loadIntervieweeStats(User currentUser, Model model) {
        try {
            List<Answer> myAnswers = answerRepository.findByUserIdWithFeedbacks(currentUser.getId());
            
            long totalAnswers = myAnswers.size();
            
            Set<Long> uniqueSessionIds = myAnswers.stream()
                .filter(a -> a.getQuestion() != null && a.getQuestion().getSession() != null)
                .map(a -> a.getQuestion().getSession().getId())
                .collect(Collectors.toSet());
            long participatedSessions = uniqueSessionIds.size();
            
            List<Feedback> allFeedbacks = myAnswers.stream()
                .flatMap(a -> a.getFeedbacks().stream())
                .collect(Collectors.toList());
            
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
            
            double avgOverallScore = scoreCount > 0 ? totalScore / scoreCount : 0.0;
            
            Map<String, Long> answersByMonth = new LinkedHashMap<>();
            DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
            for (Answer answer : myAnswers) {
                if (answer.getCreatedAt() != null) {
                    String month = answer.getCreatedAt().format(monthFormatter);
                    answersByMonth.put(month, answersByMonth.getOrDefault(month, 0L) + 1);
                }
            }
            
            Map<String, Double> scoresByMonth = new LinkedHashMap<>();
            Map<String, List<Integer>> monthlyScores = new HashMap<>();
            
            for (Answer answer : myAnswers) {
                if (answer.getCreatedAt() != null) {
                    String month = answer.getCreatedAt().format(monthFormatter);
                    
                    OptionalInt aiScore = answer.getFeedbacks().stream()
                        .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.AI && f.getScore() != null)
                        .mapToInt(Feedback::getScore)
                        .findFirst();
                    
                    OptionalInt interviewerScore = answer.getFeedbacks().stream()
                        .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.INTERVIEWER && f.getScore() != null)
                        .mapToInt(Feedback::getScore)
                        .findFirst();
                    
                    if (aiScore.isPresent()) {
                        monthlyScores.computeIfAbsent(month, k -> new ArrayList<>()).add(aiScore.getAsInt());
                    }
                    if (interviewerScore.isPresent()) {
                        monthlyScores.computeIfAbsent(month, k -> new ArrayList<>()).add(interviewerScore.getAsInt());
                    }
                }
            }
            
            monthlyScores.forEach((month, scores) -> {
                double avg = scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
                scoresByMonth.put(month, Math.round(avg * 10) / 10.0);
            });
            
            List<Map<String, Object>> recentAnswers = myAnswers.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .map(answer -> {
                    Map<String, Object> answerData = new HashMap<>();
                    answerData.put("questionText", answer.getQuestion() != null ? answer.getQuestion().getText() : "질문 없음");
                    answerData.put("answerText", answer.getAnswerText());
                    answerData.put("createdAt", answer.getCreatedAt());
                    
                    OptionalInt aiScore = answer.getFeedbacks().stream()
                        .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.AI)
                        .mapToInt(Feedback::getScore)
                        .findFirst();
                    
                    OptionalInt interviewerScore = answer.getFeedbacks().stream()
                        .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.INTERVIEWER)
                        .mapToInt(Feedback::getScore)
                        .findFirst();
                    
                    answerData.put("aiScore", aiScore.isPresent() ? aiScore.getAsInt() : null);
                    answerData.put("interviewerScore", interviewerScore.isPresent() ? interviewerScore.getAsInt() : null);
                    
                    return answerData;
                })
                .collect(Collectors.toList());
            
            List<Map<String, Object>> userRankings = calculateUserRankings(currentUser.getId());
            
            List<Map<String, Object>> growthData = new ArrayList<>();
            for (Answer answer : myAnswers) {
                if (answer.getCreatedAt() != null) {
                    Map<String, Object> dataPoint = new HashMap<>();
                    dataPoint.put("date", answer.getCreatedAt().format(DateTimeFormatter.ofPattern("MM/dd")));
                    
                    OptionalInt aiScore = answer.getFeedbacks().stream()
                        .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.AI && f.getScore() != null)
                        .mapToInt(Feedback::getScore)
                        .findFirst();
                    
                    OptionalInt interviewerScore = answer.getFeedbacks().stream()
                        .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.INTERVIEWER && f.getScore() != null)
                        .mapToInt(Feedback::getScore)
                        .findFirst();
                    
                    dataPoint.put("aiScore", aiScore.isPresent() ? aiScore.getAsInt() : null);
                    dataPoint.put("interviewerScore", interviewerScore.isPresent() ? interviewerScore.getAsInt() : null);
                    
                    growthData.add(dataPoint);
                }
            }
            
            List<String> months = new ArrayList<>(answersByMonth.keySet());
            List<Long> counts = new ArrayList<>(answersByMonth.values());
            
            long aiFeedbackCount = allFeedbacks.stream()
                .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.AI)
                .count();
            
            long interviewerFeedbackCount = allFeedbacks.stream()
                .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.INTERVIEWER)
                .count();
            
            model.addAttribute("totalAnswers", totalAnswers);
            model.addAttribute("participatedSessions", participatedSessions);
            model.addAttribute("avgAiScore", Math.round(avgAiScore * 10) / 10.0);
            model.addAttribute("avgInterviewerScore", Math.round(avgInterviewerScore * 10) / 10.0);
            model.addAttribute("avgOverallScore", Math.round(avgOverallScore * 10) / 10.0);
            model.addAttribute("answersByMonth", answersByMonth);
            model.addAttribute("scoresByMonth", scoresByMonth);
            model.addAttribute("recentAnswers", recentAnswers);
            model.addAttribute("userRankings", userRankings);
            model.addAttribute("growthData", growthData);
            model.addAttribute("months", months);
            model.addAttribute("counts", counts);
            model.addAttribute("aiFeedbackCount", aiFeedbackCount);
            model.addAttribute("interviewerFeedbackCount", interviewerFeedbackCount);
            model.addAttribute("myAnswersData", myAnswers);
            
            return "user/myStats";
            
        } catch (Exception e) {
            log.error("면접자 통계 로드 실패", e);
            model.addAttribute("error", "통계를 불러올 수 없습니다.");
            return "redirect:/auth/mypage";
        }
    }

    private List<Map<String, Object>> calculateUserRankings(Long currentUserId) {
        try {
            List<User> allUsers = userRepository.findAll();
            List<Map<String, Object>> rankings = new ArrayList<>();
            
            for (User user : allUsers) {
                Long userId = user.getId();
                List<Answer> userAnswers = answerRepository.findByUserIdWithFeedbacks(userId);
                
                if (userAnswers.isEmpty()) {
                    continue;
                }
                
                List<Feedback> allFeedbacks = userAnswers.stream()
                    .flatMap(a -> a.getFeedbacks().stream())
                    .collect(Collectors.toList());
                
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
                rankData.put("userName", user.getName());
                rankData.put("avgScore", Math.round(finalAvgScore * 10) / 10.0);
                rankData.put("answerCount", userAnswers.size());
                rankData.put("isCurrentUser", userId.equals(currentUserId));
                
                rankings.add(rankData);
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
    @Transactional(readOnly = true)
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

    @GetMapping("/auth/mypage/withdraw")
    public String withdrawPage() {
        return "user/withdraw";
    }
}