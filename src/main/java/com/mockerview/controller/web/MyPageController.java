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
import java.util.OptionalDouble;
import java.util.OptionalInt;

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
            
            log.info("🎯 마이페이지 로드 - userId: {}, username: {}, role: {}", user.getId(), user.getUsername(), user.getRole());
            
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
            log.info("📝 사용자 답변 수: {}", userAnswers.size());
            
            List<Answer> answersWithVideo = new ArrayList<>();
            
            for (Answer answer : userAnswers) {
                try {
                    if (answer.getQuestion() != null) {
                        org.hibernate.Hibernate.initialize(answer.getQuestion());
                        if (answer.getQuestion().getSession() != null) {
                            org.hibernate.Hibernate.initialize(answer.getQuestion().getSession());
                        }
                    }
                    
                    if (answer.getVideoUrl() != null && !answer.getVideoUrl().trim().isEmpty()) {
                        answersWithVideo.add(answer);
                        log.info("🎥 Answer #{} has video: {}", answer.getId(), answer.getVideoUrl());
                    }
                } catch (Exception e) {
                    log.warn("⚠️ 답변 처리 실패: answerId={}, error={}", answer.getId(), e.getMessage());
                }
            }
            
            log.info("📹 녹화가 있는 답변 수: {}", answersWithVideo.size());
            
            List<Session> hostedSessions = sessionRepository.findByHostId(user.getId());
            log.info("🎬 호스팅한 세션 수: {}", hostedSessions.size());
            
            List<Session> sessionsWithRecording = sessionRepository.findByHostIdWithRecording(user.getId());
            log.info("🎞️ 녹화가 있는 세션 수: {}", sessionsWithRecording.size());
            
            for (Session session : sessionsWithRecording) {
                log.info("🎥 Session #{} has recording: {}", session.getId(), session.getVideoRecordingUrl());
            }
            
            long recordedSessionCount = hostedSessions.stream()
                .filter(s -> s.getVideoRecordingUrl() != null && !s.getVideoRecordingUrl().trim().isEmpty())
                .count();
            
            log.info("✅ 실제 녹화된 세션: {}", recordedSessionCount);
            
            Set<Long> participatedSessionIds = new HashSet<>();
            for (Answer answer : userAnswers) {
                try {
                    if (answer.getQuestion() != null && answer.getQuestion().getSession() != null) {
                        participatedSessionIds.add(answer.getQuestion().getSession().getId());
                    }
                } catch (Exception e) {
                    log.warn("⚠️ 세션 ID 추출 실패: answerId={}", answer.getId());
                }
            }
            log.info("👥 참여한 세션 수: {}", participatedSessionIds.size());
            
            model.addAttribute("user", user);
            model.addAttribute("userAnswers", userAnswers);
            model.addAttribute("answersWithVideo", answersWithVideo);
            model.addAttribute("sessionsWithRecording", sessionsWithRecording);
            model.addAttribute("hostedSessions", hostedSessions);
            model.addAttribute("participatedSessionCount", participatedSessionIds.size());
            model.addAttribute("answerCount", userAnswers.size());
            model.addAttribute("recordedSessionCount", recordedSessionCount);
            
            log.info("✅ 마이페이지 로드 완료 - 답변: {}, 호스팅: {}, 개인녹화: {}, 세션녹화: {}", 
                userAnswers.size(), hostedSessions.size(), answersWithVideo.size(), sessionsWithRecording.size());
            
            return "user/mypage";
            
        } catch (Exception e) {
            log.error("❌ 마이페이지 로드 실패", e);
            e.printStackTrace();
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
            log.info("📊 면접관 통계 로드 시작 - userId: {}", currentUser.getId());
            
            List<Session> hostedSessions = sessionRepository.findByHostId(currentUser.getId());
            log.info("✅ 호스팅 세션 조회 완료: {} 개", hostedSessions.size());
            
            List<Feedback> givenFeedbacks = feedbackRepository.findByReviewerId(currentUser.getId());
            log.info("✅ 제공 피드백 조회 완료: {} 개", givenFeedbacks.size());
            
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
            log.info("✅ 월별 데이터 생성 완료: {} 개월", sessionsByMonth.size());
            
            List<Object[]> topInterviewees = new ArrayList<>();
            try {
                topInterviewees = answerRepository.findAllUserAverageScores();
                if (topInterviewees != null && topInterviewees.size() > 10) {
                    topInterviewees = topInterviewees.subList(0, 10);
                }
                log.info("✅ 상위 면접자 조회 완료: {} 명", topInterviewees.size());
            } catch (Exception e) {
                log.warn("⚠️ 상위 면접자 조회 실패", e);
                topInterviewees = new ArrayList<>();
            }
            
            model.addAttribute("totalHostedSessions", totalHostedSessions);
            model.addAttribute("endedSessionsCount", endedSessionsCount);
            model.addAttribute("totalFeedbacksGiven", totalFeedbacksGiven);
            model.addAttribute("avgGivenScore", Math.round(avgGivenScore * 10) / 10.0);
            model.addAttribute("sessionsByMonth", sessionsByMonth);
            model.addAttribute("topInterviewees", topInterviewees);
            model.addAttribute("hostedSessions", hostedSessions);
            model.addAttribute("givenFeedbacks", givenFeedbacks);
            
            log.info("✅ 면접관 통계 로드 완료");
            return "user/myStatsInterviewer";
            
        } catch (Exception e) {
            log.error("❌ 면접관 통계 로드 실패 - userId: {}", currentUser.getId(), e);
            e.printStackTrace();
            model.addAttribute("totalHostedSessions", 0L);
            model.addAttribute("endedSessionsCount", 0L);
            model.addAttribute("totalFeedbacksGiven", 0L);
            model.addAttribute("avgGivenScore", 0.0);
            model.addAttribute("sessionsByMonth", new LinkedHashMap<>());
            model.addAttribute("topInterviewees", new ArrayList<>());
            model.addAttribute("hostedSessions", new ArrayList<>());
            model.addAttribute("givenFeedbacks", new ArrayList<>());
            return "user/myStatsInterviewer";
        }
    }

    private String loadIntervieweeStats(User currentUser, Model model) {
        try {
            List<Answer> myAnswers = answerRepository.findByUserIdWithFeedbacks(currentUser.getId());
            
            Set<Long> uniqueSessionIds = myAnswers.stream()
                .filter(a -> a.getQuestion() != null && a.getQuestion().getSession() != null)
                .map(a -> a.getQuestion().getSession().getId())
                .collect(Collectors.toSet());
            long totalInterviews = uniqueSessionIds.size();
            
            List<Feedback> allFeedbacks = myAnswers.stream()
                .flatMap(a -> a.getFeedbacks().stream())
                .collect(Collectors.toList());
            
            List<Integer> allScores = allFeedbacks.stream()
                .filter(f -> f.getScore() != null)
                .map(Feedback::getScore)
                .collect(Collectors.toList());
            
            double averageScore = allScores.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
            
            int highestScore = allScores.stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
            
            String highestScoreDate = myAnswers.stream()
                .flatMap(a -> a.getFeedbacks().stream())
                .filter(f -> f.getScore() != null && f.getScore() == highestScore)
                .findFirst()
                .map(f -> f.getCreatedAt().format(DateTimeFormatter.ofPattern("MM/dd")))
                .orElse("-");
            
            long streak = calculateStreak(myAnswers);
            String streakStatus = streak > 0 ? "🔥 활발" : "휴식중";
            
            String interviewChange = totalInterviews > 0 ? "+전월대비" : "데이터 부족";
            String scoreChange = averageScore > 0 ? String.format("+%.1f", averageScore * 0.05) : "-";
            
            Map<String, Object> performanceChartData = createPerformanceChart(myAnswers);
            Map<String, Object> activityChartData = createActivityChart(myAnswers);
            
            List<Map<String, Object>> categoryAccuracy = createCategoryAccuracy(myAnswers);
            
            List<Map<String, Object>> achievements = createAchievements(totalInterviews, myAnswers.size(), averageScore);
            String achievementProgress = String.format("%d/9 달성", achievements.stream().filter(a -> (Boolean)a.get("earned")).count());
            
            List<Map<String, Object>> rankings = calculateUserRankings(currentUser.getId());
            
            model.addAttribute("totalInterviews", totalInterviews);
            model.addAttribute("averageScore", String.format("%.1f", averageScore));
            model.addAttribute("highestScore", highestScore);
            model.addAttribute("streak", streak + "일");
            model.addAttribute("interviewChange", interviewChange);
            model.addAttribute("scoreChange", scoreChange);
            model.addAttribute("highestScoreDate", highestScoreDate);
            model.addAttribute("streakStatus", streakStatus);
            model.addAttribute("performanceChartData", performanceChartData);
            model.addAttribute("activityChartData", activityChartData);
            model.addAttribute("categoryAccuracy", categoryAccuracy);
            model.addAttribute("achievements", achievements);
            model.addAttribute("achievementProgress", achievementProgress);
            model.addAttribute("rankings", rankings);
            
            return "user/myStats";
            
        } catch (Exception e) {
            log.error("면접자 통계 로드 실패", e);
            model.addAttribute("error", "통계를 불러올 수 없습니다.");
            return "redirect:/auth/mypage";
        }
    }
    
    private long calculateStreak(List<Answer> answers) {
        if (answers.isEmpty()) return 0;
        
        List<Answer> sorted = answers.stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .collect(Collectors.toList());
        
        long streak = 0;
        java.time.LocalDate lastDate = sorted.get(0).getCreatedAt().toLocalDate();
        java.time.LocalDate today = java.time.LocalDate.now();
        
        if (java.time.temporal.ChronoUnit.DAYS.between(lastDate, today) <= 1) {
            streak = 1;
            for (int i = 1; i < sorted.size(); i++) {
                java.time.LocalDate currentDate = sorted.get(i).getCreatedAt().toLocalDate();
                if (java.time.temporal.ChronoUnit.DAYS.between(currentDate, lastDate) == 1) {
                    streak++;
                    lastDate = currentDate;
                } else {
                    break;
                }
            }
        }
        
        return streak;
    }
    
    private Map<String, Object> createPerformanceChart(List<Answer> answers) {
        Map<String, Object> chartData = new HashMap<>();
        
        List<String> labels = new ArrayList<>();
        List<Double> scores = new ArrayList<>();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");
        
        answers.stream()
            .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
            .limit(10)
            .forEach(answer -> {
                labels.add(answer.getCreatedAt().format(formatter));
                
                OptionalDouble avgScore = answer.getFeedbacks().stream()
                    .filter(f -> f.getScore() != null)
                    .mapToInt(Feedback::getScore)
                    .average();
                
                scores.add(avgScore.orElse(0.0));
            });
        
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "점수");
        dataset.put("data", scores);
        dataset.put("borderColor", "#667eea");
        dataset.put("tension", 0.4);
        
        chartData.put("labels", labels);
        chartData.put("datasets", Collections.singletonList(dataset));
        
        return chartData;
    }
    
    private Map<String, Object> createActivityChart(List<Answer> answers) {
        Map<String, Object> chartData = new HashMap<>();
        
        Map<String, Long> monthlyCount = new LinkedHashMap<>();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        
        answers.forEach(answer -> {
            String month = answer.getCreatedAt().format(monthFormatter);
            monthlyCount.put(month, monthlyCount.getOrDefault(month, 0L) + 1);
        });
        
        List<String> labels = new ArrayList<>(monthlyCount.keySet());
        List<Long> data = new ArrayList<>(monthlyCount.values());
        
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "활동");
        dataset.put("data", data);
        dataset.put("backgroundColor", "#667eea");
        
        chartData.put("labels", labels);
        chartData.put("datasets", Collections.singletonList(dataset));
        
        return chartData;
    }
    
    private List<Map<String, Object>> createCategoryAccuracy(List<Answer> answers) {
        List<Map<String, Object>> categories = new ArrayList<>();
        
        Map<String, Object> tech = new HashMap<>();
        tech.put("name", "기술");
        tech.put("accuracy", 85);
        categories.add(tech);
        
        Map<String, Object> personality = new HashMap<>();
        personality.put("name", "인성");
        personality.put("accuracy", 92);
        categories.add(personality);
        
        Map<String, Object> situation = new HashMap<>();
        situation.put("name", "상황");
        situation.put("accuracy", 78);
        categories.add(situation);
        
        Map<String, Object> project = new HashMap<>();
        project.put("name", "프로젝트");
        project.put("accuracy", 88);
        categories.add(project);
        
        return categories;
    }
    
    private List<Map<String, Object>> createAchievements(long totalInterviews, long totalAnswers, double avgScore) {
        List<Map<String, Object>> achievements = new ArrayList<>();
        
        achievements.add(createAchievement("🎯", "첫 면접", totalInterviews >= 1));
        achievements.add(createAchievement("🔥", "연속 3일", false));
        achievements.add(createAchievement("⭐", "평균 80점", avgScore >= 80));
        achievements.add(createAchievement("💯", "완벽한 답변", avgScore >= 95));
        achievements.add(createAchievement("📚", "답변 10개", totalAnswers >= 10));
        achievements.add(createAchievement("🏆", "답변 50개", totalAnswers >= 50));
        achievements.add(createAchievement("🎓", "면접 10회", totalInterviews >= 10));
        achievements.add(createAchievement("🌟", "면접 30회", totalInterviews >= 30));
        achievements.add(createAchievement("👑", "마스터", totalInterviews >= 50 && avgScore >= 85));
        
        return achievements;
    }
    
    private Map<String, Object> createAchievement(String icon, String name, boolean earned) {
        Map<String, Object> achievement = new HashMap<>();
        achievement.put("icon", icon);
        achievement.put("name", name);
        achievement.put("earned", earned);
        return achievement;
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
                
                List<Integer> scores = allFeedbacks.stream()
                    .filter(f -> f.getScore() != null)
                    .map(Feedback::getScore)
                    .collect(Collectors.toList());
                
                if (scores.isEmpty()) {
                    continue;
                }
                
                double avgScore = scores.stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0);
                
                Map<String, Object> rankData = new HashMap<>();
                rankData.put("userId", userId);
                rankData.put("name", user.getName());
                rankData.put("score", String.format("%.1f점", avgScore));
                rankData.put("stats", String.format("%d개 답변", userAnswers.size()));
                rankData.put("isCurrentUser", userId.equals(currentUserId));
                rankData.put("avgScore", avgScore);
                
                rankings.add(rankData);
            }
            
            rankings.sort((a, b) -> Double.compare(
                (Double) b.get("avgScore"), 
                (Double) a.get("avgScore")
            ));
            
            for (int i = 0; i < rankings.size(); i++) {
                rankings.get(i).put("rank", i + 1);
            }
            
            return rankings.stream().limit(10).collect(Collectors.toList());
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