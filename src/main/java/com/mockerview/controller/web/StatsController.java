package com.mockerview.controller.web;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.Answer;
import com.mockerview.entity.Question;
import com.mockerview.entity.Session;
import com.mockerview.entity.User;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.QuestionRepository;
import com.mockerview.repository.SessionRepository;
import com.mockerview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class StatsController {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;

    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public String showStats(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            log.info("===== 통계 페이지 로드 시작 =====");
            log.info("사용자: {}, 역할: {}", currentUser.getName(), currentUser.getRole());
            
            model.addAttribute("currentUser", currentUser);
            
            if (currentUser.getRole() == User.Role.HOST || currentUser.getRole() == User.Role.REVIEWER) {
                return loadInterviewerStats(model, currentUser);
            } else {
                return loadStudentStats(model, currentUser);
            }
        } catch (Exception e) {
            log.error("❌ 통계 페이지 로드 실패", e);
            model.addAttribute("error", "통계를 불러오는데 실패했습니다: " + e.getMessage());
            return "redirect:/auth/mypage";
        }
    }

    private String loadStudentStats(Model model, User currentUser) {
        try {
            List<Answer> userAnswers = answerRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
            
            for (Answer answer : userAnswers) {
                if (answer.getQuestion() != null) {
                    answer.getQuestion().getText();
                    if (answer.getQuestion().getSession() != null) {
                        answer.getQuestion().getSession().getTitle();
                    }
                }
                answer.getAnswerText();
            }
            
            long totalSessions = answerRepository.countDistinctSessionsByUserId(currentUser.getId());
            long totalAnswers = userAnswers.size();
            
            List<Answer> answersWithScore = userAnswers.stream()
                .filter(a -> a.getScore() != null)
                .collect(Collectors.toList());
            
            double avgScore = 0.0;
            if (!answersWithScore.isEmpty()) {
                avgScore = answersWithScore.stream()
                    .mapToInt(Answer::getScore)
                    .average()
                    .orElse(0.0);
            }
            
            Map<String, Long> categoryStats = new HashMap<>();
            for (Answer answer : userAnswers) {
                if (answer.getQuestion() != null && answer.getQuestion().getSession() != null) {
                    String category = answer.getQuestion().getSession().getCategory();
                    if (category != null) {
                        categoryStats.put(category, categoryStats.getOrDefault(category, 0L) + 1);
                    }
                }
            }
            
            List<RankingDTO> rankings = new ArrayList<>();
            
            model.addAttribute("totalSessions", totalSessions);
            model.addAttribute("totalAnswers", totalAnswers);
            model.addAttribute("avgScore", String.format("%.1f", avgScore));
            model.addAttribute("recentAnswers", userAnswers.stream().limit(5).collect(Collectors.toList()));
            model.addAttribute("categoryStats", categoryStats);
            model.addAttribute("rankings", rankings);
            
            log.info("✅ 학생 통계 로드 완료 - 세션: {}, 답변: {}, 평균: {}", totalSessions, totalAnswers, avgScore);
            
            return "user/myStats";
        } catch (Exception e) {
            log.error("❌ 학생 통계 로드 실패", e);
            throw e;
        }
    }

    private String loadInterviewerStats(Model model, User currentUser) {
        try {
            List<Session> hostedSessions = sessionRepository.findByHostId(currentUser.getId());
            
            for (Session session : hostedSessions) {
                session.getTitle();
                if (session.getQuestions() != null) {
                    session.getQuestions().size();
                }
            }
            
            long totalSessions = hostedSessions.size();
            
            Long totalQuestions = questionRepository.countByQuestionerIdAndSessionIsSelfInterview(
                currentUser.getId(), "N");
            
            List<Session> recentSessions = hostedSessions.stream()
                .sorted(Comparator.comparing(Session::getCreatedAt).reversed())
                .limit(5)
                .collect(Collectors.toList());
            
            Map<Session.SessionStatus, Long> statusCounts = hostedSessions.stream()
                .collect(Collectors.groupingBy(Session::getSessionStatus, Collectors.counting()));
            
            long activeSessions = statusCounts.getOrDefault(Session.SessionStatus.RUNNING, 0L);
            long endedSessions = statusCounts.getOrDefault(Session.SessionStatus.ENDED, 0L);
            long plannedSessions = statusCounts.getOrDefault(Session.SessionStatus.PLANNED, 0L);
            
            model.addAttribute("totalSessions", totalSessions);
            model.addAttribute("totalQuestions", totalQuestions);
            model.addAttribute("activeSessions", activeSessions);
            model.addAttribute("endedSessions", endedSessions);
            model.addAttribute("plannedSessions", plannedSessions);
            model.addAttribute("recentSessions", recentSessions);
            model.addAttribute("topInterviewees", new ArrayList<>());
            
            log.info("✅ 면접관 통계 로드 완료 - 세션: {}, 질문: {}", totalSessions, totalQuestions);
            
            return "user/myStatsInterviewer";
        } catch (Exception e) {
            log.error("❌ 면접관 통계 로드 실패", e);
            throw e;
        }
    }

    public static class RankingDTO {
        private String userName;
        private Long totalAnswers;
        private Double avgScore;
        private Integer rank;

        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        
        public Long getTotalAnswers() { return totalAnswers; }
        public void setTotalAnswers(Long totalAnswers) { this.totalAnswers = totalAnswers; }
        
        public Double getAvgScore() { return avgScore; }
        public void setAvgScore(Double avgScore) { this.avgScore = avgScore; }
        
        public Integer getRank() { return rank; }
        public void setRank(Integer rank) { this.rank = rank; }
    }
}