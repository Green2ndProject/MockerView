package com.mockerview.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockerview.dto.AchievementDTO;
import com.mockerview.dto.CategoryScoreDTO;
import com.mockerview.dto.RankingDTO;
import com.mockerview.dto.StatisticsDTO;
import com.mockerview.entity.Answer;
import com.mockerview.entity.Feedback;
import com.mockerview.entity.InterviewMBTI;
import com.mockerview.entity.Session;
import com.mockerview.entity.SelfInterviewReport;
import com.mockerview.entity.User;
import com.mockerview.exception.AlreadyDeletedException;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.FeedbackRepository;
import com.mockerview.repository.InterviewMBTIRepository;
import com.mockerview.repository.SessionRepository;
import com.mockerview.repository.SelfInterviewReportRepository;
import com.mockerview.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRepository sessionRepository;
    private final AnswerRepository answerRepository;
    private final FeedbackRepository feedbackRepository;
    private final InterviewMBTIRepository mbtiRepository;
    private final SelfInterviewReportRepository selfInterviewReportRepository;

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }
    
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }
    
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }
    
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }
    
    @Transactional
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public void withdraw(String username, String password, String reason) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        
        if(user.getIsDeleted() == 1){
            throw new AlreadyDeletedException("이미 탈퇴 처리된 계정입니다");
        }

        if(!passwordEncoder.matches(password, user.getPassword())){
            log.warn("탈퇴 로직 실패 - 비밀번호 불일치");
            throw new IllegalArgumentException("비밀번호가 틀렸습니다");
        }

        Long id = user.getId();
        long timestamp = (System.currentTimeMillis() / 1000);
        String anonymizedEmail = String.format("del_%d_%d@mvr.invalid", id, timestamp);
        String anonymizedUsername = String.format("del_user_%d_%d", id, timestamp);
        
        log.info("탈퇴 로직 - Service 진입 성공."); 
        log.info("탈퇴 로직 성공 - Soft Delete 처리 시작"); 
 
        user.setDeletedAt(LocalDateTime.now());
        user.setPassword("invalid_deleted_hash_" + id);
        user.setWithdrawalReason(reason);
        user.setEmail(anonymizedEmail);
        user.setUsername(anonymizedUsername);
        user.setName("탈퇴회원");
        user.setIsDeleted(1);

        userRepository.save(user);

        log.info("탈퇴 로직 최종 완료 및 DB 반영");
    }

    @Transactional(readOnly = true)
    public String findUsername(String name, String email) {
        User user = userRepository.findByNameAndEmail(name, email)
            .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보가 없습니다."));
        return user.getUsername();
    }

    @Transactional
    public void resetPassword(String username, String email, String newPassword) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));
        
        if (user.getIsDeleted() == 1) {
            throw new IllegalArgumentException("탈퇴한 회원입니다.");
        }
        
        if (!user.getEmail().equals(email)) {
            throw new IllegalArgumentException("이메일이 일치하지 않습니다.");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public List<UserSearchResponse> searchUsers(String keyword){

        if(keyword == null || keyword.trim().isEmpty()){
            return Collections.emptyList();
        }

        List<User> users = 
            userRepository.findByNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(keyword, keyword);

        return users.stream().map(UserSearchResponse::from).collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public StatisticsDTO getUserStatistics(Long userId) {
        try {
            log.info("📊 통계 수집 시작 - userId: {}", userId);

            List<Session> normalSessions = sessionRepository.findByHostAndIsSelfInterviewOrderByCreatedAtDesc(
                userRepository.findById(userId).orElseThrow(), "N"
            );
            List<Session> selfSessions = sessionRepository.findSelfInterviewsByUserId(userId);
            
            List<SelfInterviewReport> selfReports = selfInterviewReportRepository.findByUserIdOrderByCreatedAtDesc(userId);
            
            int totalSessions = normalSessions.size() + selfSessions.size() + selfReports.size();
            
            long completedCount = normalSessions.stream()
                .filter(s -> s.getSessionStatus() == Session.SessionStatus.ENDED)
                .count();
            completedCount += selfSessions.stream()
                .filter(s -> s.getSessionStatus() == Session.SessionStatus.ENDED)
                .count();
            completedCount += selfReports.size();
            
            List<Answer> answers = answerRepository.findByUserIdOrderByCreatedAtDesc(userId);
            int totalAnswers = answers.size();
            totalAnswers += selfReports.stream()
                .mapToInt(SelfInterviewReport::getTotalQuestions)
                .sum();
            
            List<Feedback> feedbacks = feedbackRepository.findByAnswerIn(answers);
            int totalFeedbacks = feedbacks.size();
            totalFeedbacks += selfReports.size();
            
            double averageScore = answers.stream()
                .filter(a -> a.getScore() != null && a.getScore() > 0)
                .mapToInt(Answer::getScore)
                .average()
                .orElse(0.0);
            
            double selfReportAvg = selfReports.stream()
                .filter(r -> r.getOverallAvg() != null)
                .mapToDouble(SelfInterviewReport::getOverallAvg)
                .average()
                .orElse(0.0);
            
            if (averageScore > 0 && selfReportAvg > 0) {
                averageScore = (averageScore + selfReportAvg) / 2.0;
            } else if (selfReportAvg > 0) {
                averageScore = selfReportAvg;
            }
            
            String mbtiType = mbtiRepository.findLatestByUserId(userId)
                .map(InterviewMBTI::getMbtiType)
                .orElse("미분석");
            
            Map<String, Integer> monthlyProgress = calculateMonthlyProgress(normalSessions, selfSessions, selfReports);
            
            List<CategoryScoreDTO> categoryScores = calculateCategoryScores(answers, selfReports);

            StatisticsDTO stats = StatisticsDTO.builder()
                .totalSessions(totalSessions)
                .completedSessions((int) completedCount)
                .totalAnswers(totalAnswers)
                .totalFeedbacks(totalFeedbacks)
                .averageScore(Math.round(averageScore * 10) / 10.0)
                .mbtiType(mbtiType)
                .monthlyProgress(monthlyProgress)
                .categoryScores(categoryScores)
                .build();

            log.info("✅ 통계 수집 완료 - 총 세션: {}, 답변: {}, 피드백: {}, 평균 점수: {}", 
                totalSessions, totalAnswers, totalFeedbacks, averageScore);

            return stats;
        } catch (Exception e) {
            log.error("❌ 통계 수집 실패", e);
            return StatisticsDTO.builder()
                .totalSessions(0)
                .completedSessions(0)
                .totalAnswers(0)
                .totalFeedbacks(0)
                .averageScore(0.0)
                .mbtiType("미분석")
                .monthlyProgress(new HashMap<>())
                .categoryScores(List.of())
                .build();
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getInterviewerStatistics(Long userId) {
        try {
            log.info("📊 면접관 통계 수집 시작 - userId: {}", userId);

            List<Session> hostedSessions = sessionRepository.findByHostId(userId);
            
            long endedSessions = hostedSessions.stream()
                .filter(s -> s.getSessionStatus() == Session.SessionStatus.ENDED)
                .count();
            
            List<Long> sessionIds = hostedSessions.stream()
                .map(Session::getId)
                .collect(Collectors.toList());
            
            long totalFeedbacks = 0;
            for (Long sessionId : sessionIds) {
                List<Answer> sessionAnswers = answerRepository.findBySessionIdOrderByCreatedAt(sessionId);
                totalFeedbacks += feedbackRepository.findByAnswerIn(sessionAnswers).size();
            }
            
            Map<String, Integer> sessionsByMonth = new HashMap<>();
            for (Session session : hostedSessions) {
                if (session.getCreatedAt() != null) {
                    YearMonth ym = YearMonth.from(session.getCreatedAt());
                    String key = ym.toString();
                    sessionsByMonth.merge(key, 1, Integer::sum);
                }
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalHostedSessions", hostedSessions.size());
            stats.put("endedSessionsCount", endedSessions);
            stats.put("totalFeedbacksGiven", totalFeedbacks);
            stats.put("sessionsByMonth", sessionsByMonth);

            log.info("✅ 면접관 통계 수집 완료 - 총 세션: {}, 종료: {}, 피드백: {}", 
                hostedSessions.size(), endedSessions, totalFeedbacks);

            return stats;
        } catch (Exception e) {
            log.error("❌ 면접관 통계 수집 실패", e);
            Map<String, Object> emptyStats = new HashMap<>();
            emptyStats.put("totalHostedSessions", 0);
            emptyStats.put("endedSessionsCount", 0);
            emptyStats.put("totalFeedbacksGiven", 0);
            emptyStats.put("sessionsByMonth", new HashMap<>());
            return emptyStats;
        }
    }

    @Transactional(readOnly = true)
    public List<AchievementDTO> getUserAchievements(Long userId) {
        try {
            StatisticsDTO stats = getUserStatistics(userId);
            boolean hasMbti = !stats.getMbtiType().equals("미분석");
            
            List<Answer> answers = answerRepository.findByUserIdOrderByCreatedAtDesc(userId);
            boolean hasPerfectScore = answers.stream().anyMatch(a -> a.getScore() != null && a.getScore() == 5);
            
            List<Session> allSessions = new ArrayList<>();
            allSessions.addAll(sessionRepository.findByHostAndIsSelfInterviewOrderByCreatedAtDesc(
                userRepository.findById(userId).orElseThrow(), "N"
            ));
            allSessions.addAll(sessionRepository.findSelfInterviewsByUserId(userId));
            
            boolean hasEarlyBird = allSessions.stream()
                .anyMatch(s -> s.getCreatedAt() != null && s.getCreatedAt().getHour() < 6);
            boolean hasNightOwl = allSessions.stream()
                .anyMatch(s -> s.getCreatedAt() != null && s.getCreatedAt().getHour() >= 22);
            
            Map<String, Integer> monthlyProgress = stats.getMonthlyProgress();
            boolean hasMonthlyChallenge = monthlyProgress.values().stream().anyMatch(count -> count >= 10);
            
            List<AchievementDTO> achievements = new ArrayList<>();
            
            achievements.add(AchievementDTO.builder()
                .icon("🎯")
                .name("첫걸음")
                .description("첫 면접 완료")
                .earned(stats.getTotalSessions() >= 1)
                .progress(Math.min(stats.getTotalSessions(), 1))
                .target(1)
                .build());
            
            achievements.add(AchievementDTO.builder()
                .icon("🔥")
                .name("열정")
                .description("10회 면접 달성")
                .earned(stats.getTotalSessions() >= 10)
                .progress(Math.min(stats.getTotalSessions(), 10))
                .target(10)
                .build());
            
            achievements.add(AchievementDTO.builder()
                .icon("💎")
                .name("백전노장")
                .description("50회 면접 달성")
                .earned(stats.getTotalSessions() >= 50)
                .progress(Math.min(stats.getTotalSessions(), 50))
                .target(50)
                .build());
            
            achievements.add(AchievementDTO.builder()
                .icon("🏆")
                .name("완벽주의자")
                .description("만점 달성하기")
                .earned(hasPerfectScore)
                .progress(hasPerfectScore ? 1 : 0)
                .target(1)
                .build());
            
            achievements.add(AchievementDTO.builder()
                .icon("⭐")
                .name("우수상")
                .description("평균 80점 이상")
                .earned(stats.getAverageScore() >= 80)
                .progress((int) Math.min(stats.getAverageScore(), 80))
                .target(80)
                .build());
            
            achievements.add(AchievementDTO.builder()
                .icon("🧠")
                .name("자기탐구자")
                .description("MBTI 분석 완료")
                .earned(hasMbti)
                .progress(hasMbti ? 1 : 0)
                .target(1)
                .build());
            
            achievements.add(AchievementDTO.builder()
                .icon("🗣️")
                .name("수다쟁이")
                .description("답변 50개 이상")
                .earned(stats.getTotalAnswers() >= 50)
                .progress(Math.min(stats.getTotalAnswers(), 50))
                .target(50)
                .build());
            
            achievements.add(AchievementDTO.builder()
                .icon("🌅")
                .name("새벽형 인간")
                .description("새벽 6시 전 면접")
                .earned(hasEarlyBird)
                .progress(hasEarlyBird ? 1 : 0)
                .target(1)
                .build());
            
            achievements.add(AchievementDTO.builder()
                .icon("🌙")
                .name("야행성")
                .description("밤 10시 이후 면접")
                .earned(hasNightOwl)
                .progress(hasNightOwl ? 1 : 0)
                .target(1)
                .build());
            
            log.info("✅ 업적 조회 완료 - 총 {}개, 획득 {}개", 
                achievements.size(), 
                achievements.stream().filter(AchievementDTO::isEarned).count());
            
            return achievements;
        } catch (Exception e) {
            log.error("❌ 업적 조회 실패", e);
            return new ArrayList<>();
        }
    }

    @Transactional(readOnly = true)
    public List<RankingDTO> getGlobalRankings(Long currentUserId, String period) {
        try {
            List<User> allUsers = userRepository.findAll().stream()
                .filter(u -> u.getIsDeleted() == 0)
                .collect(Collectors.toList());
            
            Map<Long, Integer> userScores = new HashMap<>();
            
            for (User user : allUsers) {
                StatisticsDTO stats = getUserStatistics(user.getId());
                int score = stats.getTotalSessions();
                userScores.put(user.getId(), score);
            }
            
            List<RankingDTO> rankings = userScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(10)
                .map(entry -> {
                    User user = findById(entry.getKey());
                    StatisticsDTO stats = getUserStatistics(user.getId());
                    return RankingDTO.builder()
                        .name(user.getName())
                        .stats(String.format("면접 %d회 · 평균 %.1f점", 
                            stats.getTotalSessions(), 
                            stats.getAverageScore()))
                        .score(entry.getValue() + "회")
                        .isCurrentUser(user.getId().equals(currentUserId))
                        .build();
                })
                .collect(Collectors.toList());
            
            for (int i = 0; i < rankings.size(); i++) {
                rankings.get(i).setRank(i + 1);
            }
            
            log.info("✅ 랭킹 조회 완료 - 총 {}명", rankings.size());
            
            return rankings;
        } catch (Exception e) {
            log.error("❌ 랭킹 조회 실패", e);
            return new ArrayList<>();
        }
    }

    private Map<String, Integer> calculateMonthlyProgress(List<Session> normalSessions, List<Session> selfSessions, List<SelfInterviewReport> selfReports) {
        Map<String, Integer> monthlyMap = new HashMap<>();
        
        for (Session session : normalSessions) {
            if (session.getCreatedAt() != null) {
                YearMonth ym = YearMonth.from(session.getCreatedAt());
                monthlyMap.merge(ym.toString(), 1, Integer::sum);
            }
        }
        
        for (Session session : selfSessions) {
            if (session.getCreatedAt() != null) {
                YearMonth ym = YearMonth.from(session.getCreatedAt());
                monthlyMap.merge(ym.toString(), 1, Integer::sum);
            }
        }
        
        for (SelfInterviewReport report : selfReports) {
            if (report.getCreatedAt() != null) {
                YearMonth ym = YearMonth.from(report.getCreatedAt());
                monthlyMap.merge(ym.toString(), 1, Integer::sum);
            }
        }
        
        return monthlyMap;
    }

    private List<CategoryScoreDTO> calculateCategoryScores(List<Answer> answers, List<SelfInterviewReport> selfReports) {
        Map<String, List<Double>> categoryScores = new HashMap<>();
        
        for (Answer answer : answers) {
            if (answer.getScore() != null && answer.getScore() > 0) {
                String category = answer.getQuestion().getSession().getCategory();
                if (category == null || category.isEmpty()) {
                    category = "일반";
                }
                categoryScores.computeIfAbsent(category, k -> new java.util.ArrayList<>())
                    .add((double) answer.getScore());
            }
        }
        
        for (SelfInterviewReport report : selfReports) {
            if (report.getOverallAvg() != null && report.getOverallAvg() > 0) {
                String category = report.getCategoryName();
                if (category == null || category.isEmpty()) {
                    category = "일반";
                }
                categoryScores.computeIfAbsent(category, k -> new java.util.ArrayList<>())
                    .add(report.getOverallAvg());
            }
        }
        
        return categoryScores.entrySet().stream()
            .map(entry -> {
                double avg = entry.getValue().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
                return CategoryScoreDTO.builder()
                    .category(entry.getKey())
                    .accuracy(Math.round(avg * 10) / 10.0)
                    .count(entry.getValue().size())
                    .build();
            })
            .collect(Collectors.toList());
    }
    
}
