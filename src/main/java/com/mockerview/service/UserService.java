package com.mockerview.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockerview.dto.UserSearchResponse;
import com.mockerview.dto.CategoryScoreDTO;
import com.mockerview.dto.StatisticsDTO;
import com.mockerview.entity.Answer;
import com.mockerview.entity.Feedback;
import com.mockerview.entity.InterviewMBTI;
import com.mockerview.entity.Session;
import com.mockerview.entity.User;
import com.mockerview.exception.AlreadyDeletedException;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.FeedbackRepository;
import com.mockerview.repository.InterviewMBTIRepository;
import com.mockerview.repository.SessionRepository;
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
            
            int totalSessions = normalSessions.size() + selfSessions.size();
            
            long completedCount = normalSessions.stream()
                .filter(s -> s.getSessionStatus() == Session.SessionStatus.ENDED)
                .count();
            completedCount += selfSessions.stream()
                .filter(s -> s.getSessionStatus() == Session.SessionStatus.ENDED)
                .count();
            
            List<Answer> answers = answerRepository.findByUserIdOrderByCreatedAtDesc(userId);
            int totalAnswers = answers.size();
            
            List<Feedback> feedbacks = feedbackRepository.findByAnswerIn(answers);
            int totalFeedbacks = feedbacks.size();
            
            double averageScore = answers.stream()
                .filter(a -> a.getScore() != null && a.getScore() > 0)
                .mapToInt(Answer::getScore)
                .average()
                .orElse(0.0);
            
            String mbtiType = mbtiRepository.findLatestByUserId(userId)
                .map(InterviewMBTI::getMbtiType)
                .orElse("미분석");
            
            Map<String, Integer> monthlyProgress = calculateMonthlyProgress(normalSessions, selfSessions);
            
            List<CategoryScoreDTO> categoryScores = calculateCategoryScores(answers);

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

    private Map<String, Integer> calculateMonthlyProgress(List<Session> normalSessions, List<Session> selfSessions) {
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
        
        return monthlyMap;
    }

    private List<CategoryScoreDTO> calculateCategoryScores(List<Answer> answers) {
        Map<String, List<Integer>> categoryScores = new HashMap<>();
        
        for (Answer answer : answers) {
            if (answer.getScore() != null && answer.getScore() > 0) {
                String category = answer.getQuestion().getSession().getCategory();
                if (category == null || category.isEmpty()) {
                    category = "일반";
                }
                categoryScores.computeIfAbsent(category, k -> new java.util.ArrayList<>())
                    .add(answer.getScore());
            }
        }
        
        return categoryScores.entrySet().stream()
            .map(entry -> {
                double avg = entry.getValue().stream()
                    .mapToInt(Integer::intValue)
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

