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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
    private final InterviewReportRepository interviewReportRepository;
    private final UserBadgeRepository userBadgeRepository;
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
            
            List<Answer> myAnswers = answerRepository.findByUserIdWithFeedbacks(currentUser.getId());
            
            Map<String, Object> performanceChartData = generatePerformanceChartData(myAnswers);
            model.addAttribute("performanceChartData", performanceChartData);
            
            Map<String, Object> activityChartData = generateActivityChartData(myAnswers);
            model.addAttribute("activityChartData", activityChartData);
            
            List<Map<String, Object>> categoryAccuracy = calculateCategoryAccuracy(myAnswers);
            model.addAttribute("categoryAccuracy", categoryAccuracy);
            
            List<Map<String, Object>> achievements = getAchievements(currentUser.getId(), myAnswers.size());
            model.addAttribute("achievements", achievements);
            model.addAttribute("achievementProgress", String.format("%d/%d", 
                achievements.stream().filter(a -> (Boolean)a.get("earned")).count(), achievements.size()));
            
            List<Map<String, Object>> rankings = calculateGlobalRanking(currentUser.getId());
            model.addAttribute("rankings", rankings);
            
            List<Feedback> allFeedbacks = myAnswers.stream()
                .flatMap(a -> a.getFeedbacks().stream())
                .collect(Collectors.toList());
            
            List<Integer> scores = allFeedbacks.stream()
                .filter(f -> f.getScore() != null)
                .map(Feedback::getScore)
                .collect(Collectors.toList());
            
            double avgScore = scores.isEmpty() ? 0.0 : 
                scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            
            int maxScore = scores.isEmpty() ? 0 : 
                scores.stream().mapToInt(Integer::intValue).max().orElse(0);
            
            Set<Long> participatedSessions = myAnswers.stream()
                .map(a -> a.getQuestion().getSession().getId())
                .collect(Collectors.toSet());
            
            int streak = calculateStreak(myAnswers);
            
            model.addAttribute("totalInterviews", participatedSessions.size());
            model.addAttribute("averageScore", String.format("%.1f", avgScore));
            model.addAttribute("highestScore", maxScore);
            model.addAttribute("streak", streak);
            
            model.addAttribute("interviewChange", "+3 (이번 달)");
            model.addAttribute("scoreChange", "+5.2");
            model.addAttribute("highestScoreDate", "2주 전");
            model.addAttribute("streakStatus", "연속 중");
            
            return "user/myStats";
            
        } catch (Exception e) {
            log.error("Stats 페이지 오류: ", e);
            model.addAttribute("error", "통계를 불러올 수 없습니다.");
            return "error";
        }
    }

    private Map<String, Object> generatePerformanceChartData(List<Answer> answers) {
        Map<String, Object> chartData = new HashMap<>();
        
        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();
        
        Map<LocalDate, List<Integer>> scoresByDate = answers.stream()
            .filter(a -> a.getCreatedAt() != null)
            .collect(Collectors.groupingBy(
                a -> a.getCreatedAt().toLocalDate(),
                Collectors.flatMapping(
                    a -> a.getFeedbacks().stream()
                        .filter(f -> f.getScore() != null)
                        .map(Feedback::getScore),
                    Collectors.toList()
                )
            ));
        
        scoresByDate.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .limit(10)
            .forEach(entry -> {
                labels.add(entry.getKey().format(DateTimeFormatter.ofPattern("MM/dd")));
                double avg = entry.getValue().stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0);
                data.add(avg);
            });
        
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "면접 점수");
        dataset.put("data", data);
        dataset.put("borderColor", "#667eea");
        dataset.put("backgroundColor", "rgba(102, 126, 234, 0.1)");
        dataset.put("tension", 0.4);
        
        chartData.put("labels", labels);
        chartData.put("datasets", Collections.singletonList(dataset));
        
        return chartData;
    }

    private Map<String, Object> generateActivityChartData(List<Answer> answers) {
        Map<String, Object> chartData = new HashMap<>();
        
        List<String> labels = new ArrayList<>();
        List<Integer> data = new ArrayList<>();
        
        Map<Integer, Long> answersByMonth = answers.stream()
            .filter(a -> a.getCreatedAt() != null)
            .collect(Collectors.groupingBy(
                a -> a.getCreatedAt().getMonthValue(),
                Collectors.counting()
            ));
        
        for (int i = 1; i <= 12; i++) {
            labels.add(i + "월");
            data.add(answersByMonth.getOrDefault(i, 0L).intValue());
        }
        
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "답변 수");
        dataset.put("data", data);
        dataset.put("backgroundColor", "#667eea");
        
        chartData.put("labels", labels);
        chartData.put("datasets", Collections.singletonList(dataset));
        
        return chartData;
    }

    private List<Map<String, Object>> calculateCategoryAccuracy(List<Answer> answers) {
        Map<String, List<Integer>> scoresByCategory = new HashMap<>();
        
        for (Answer answer : answers) {
            try {
                if (answer.getQuestion() != null && answer.getQuestion().getSession() != null) {
                    String category = answer.getQuestion().getSession().getCategory();
                    if (category == null) category = "기타";
                    
                    List<Integer> scores = answer.getFeedbacks().stream()
                        .filter(f -> f.getScore() != null)
                        .map(Feedback::getScore)
                        .collect(Collectors.toList());
                    
                    scoresByCategory.computeIfAbsent(category, k -> new ArrayList<>()).addAll(scores);
                }
            } catch (Exception e) {
                log.warn("카테고리 정확도 계산 실패", e);
            }
        }
        
        return scoresByCategory.entrySet().stream()
            .map(entry -> {
                Map<String, Object> cat = new HashMap<>();
                cat.put("name", entry.getKey());
                double avg = entry.getValue().stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0);
                cat.put("accuracy", (int) avg);
                return cat;
            })
            .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getAchievements(Long userId, int answerCount) {
    List<Map<String, Object>> achievements = new ArrayList<>();
    
    List<UserBadge> userBadges = userBadgeRepository.findByUserId(userId);
    Set<BadgeType> earnedBadges = userBadges.stream()
        .map(UserBadge::getBadgeType)
        .collect(Collectors.toSet());
    
    List<Answer> allAnswers = answerRepository.findByUserIdWithFeedbacks(userId);
    
    List<Integer> scores = allAnswers.stream()
        .flatMap(a -> a.getFeedbacks().stream())
        .filter(f -> f.getScore() != null)
        .map(Feedback::getScore)
        .collect(Collectors.toList());
    
    boolean hasHighScore = scores.stream().anyMatch(s -> s >= 95);
    boolean hasPerfectScore = scores.stream().anyMatch(s -> s >= 90);
    
    int streak = calculateStreak(allAnswers);
    
    Set<Long> uniqueSessions = allAnswers.stream()
        .filter(a -> a.getQuestion() != null && a.getQuestion().getSession() != null)
        .map(a -> a.getQuestion().getSession().getId())
        .collect(Collectors.toSet());
    
    boolean hasEarlyBird = allAnswers.stream()
        .anyMatch(a -> a.getCreatedAt().getHour() < 7);
    
    boolean hasNightOwl = allAnswers.stream()
        .anyMatch(a -> a.getCreatedAt().getHour() >= 23);
    
    achievements.add(createAchievement("첫 면접 완료", "🎯", 
        earnedBadges.contains(BadgeType.FIRST_INTERVIEW) || answerCount >= 1, 
        "첫 번째 면접을 완료했습니다"));
    
    achievements.add(createAchievement("7일 연속", "🔥", 
        earnedBadges.contains(BadgeType.STREAK_7) || streak >= 7, 
        "7일 연속 면접 진행"));
    
    achievements.add(createAchievement("50회 달성", "🎖️", 
        earnedBadges.contains(BadgeType.INTERVIEW_50) || answerCount >= 50, 
        "면접 50회 완료"));
    
    achievements.add(createAchievement("완벽한 면접", "💯", 
        earnedBadges.contains(BadgeType.PERFECT_SCORE) || hasPerfectScore, 
        "90점 이상 획득"));
    
    achievements.add(createAchievement("기술 전문가", "💻", 
        earnedBadges.contains(BadgeType.TECHNICAL_EXPERT) || hasHighScore, 
        "기술 점수 95점 이상"));
    
    achievements.add(createAchievement("얼리버드", "🌅", 
        earnedBadges.contains(BadgeType.EARLY_BIRD) || hasEarlyBird, 
        "오전 7시 전에 면접 진행"));
    
    achievements.add(createAchievement("올빼미", "🦉", 
        earnedBadges.contains(BadgeType.NIGHT_OWL) || hasNightOwl, 
        "밤 11시 이후 면접 진행"));
    
    achievements.add(createAchievement("10회 달성", "🏆", 
        earnedBadges.contains(BadgeType.INTERVIEW_10) || answerCount >= 10, 
        "면접 10회 완료"));
    
    achievements.add(createAchievement("100회 달성", "👑", 
        earnedBadges.contains(BadgeType.INTERVIEW_100) || answerCount >= 100, 
        "면접 100회 완료"));
    
    return achievements;
}

private Map<String, Object> createAchievement(String name, String icon, boolean earned, String description) {
    Map<String, Object> achievement = new HashMap<>();
    achievement.put("name", name);
    achievement.put("icon", icon);
    achievement.put("earned", earned);
    achievement.put("description", description);
    return achievement;
}

    private int calculateStreak(List<Answer> answers) {
        if (answers.isEmpty()) return 0;
        
        List<LocalDate> dates = answers.stream()
            .map(a -> a.getCreatedAt().toLocalDate())
            .distinct()
            .sorted(Comparator.reverseOrder())
            .collect(Collectors.toList());
        
        int streak = 1;
        LocalDate today = LocalDate.now();
        
        if (dates.isEmpty() || ChronoUnit.DAYS.between(dates.get(0), today) > 1) {
            return 0;
        }
        
        for (int i = 0; i < dates.size() - 1; i++) {
            long daysBetween = ChronoUnit.DAYS.between(dates.get(i + 1), dates.get(i));
            if (daysBetween == 1) {
                streak++;
            } else {
                break;
            }
        }
        
        return streak;
    }

    private List<Map<String, Object>> calculateGlobalRanking(Long currentUserId) {
        try {
            List<User> allUsers = userRepository.findAll();
            List<Map<String, Object>> rankings = new ArrayList<>();
            
            for (User user : allUsers) {
                if (user.getIsDeleted() == 1) {
                    continue;
                }
                
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

    @GetMapping("/auth/mypage/reports")
    @Transactional(readOnly = true)
    public String reportsPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        try {
            if (userDetails == null) {
                return "redirect:/auth/login";
            }
            
            User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            
            List<InterviewReport> reports = interviewReportRepository.findByUserOrderByCreatedAtDesc(user);
            
            log.info("📊 사용자 {}의 레포트 {}개 조회 완료", user.getUsername(), reports.size());
            
            model.addAttribute("user", user);
            model.addAttribute("reports", reports);
            model.addAttribute("reportCount", reports.size());
            
            return "mypage-reports";
            
        } catch (Exception e) {
            log.error("레포트 페이지 로드 실패", e);
            model.addAttribute("error", "레포트를 불러올 수 없습니다: " + e.getMessage());
            return "error";
        }
    }
}
