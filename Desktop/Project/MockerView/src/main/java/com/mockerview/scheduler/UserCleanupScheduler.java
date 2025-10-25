package com.mockerview.scheduler;

import com.mockerview.entity.User;
import com.mockerview.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCleanupScheduler {

    private final UserRepository userRepository;
    private final AnswerRepository answerRepository;
    private final FeedbackRepository feedbackRepository;
    private final QuestionRepository questionRepository;
    private final SessionRepository sessionRepository;
    private final ReviewRepository reviewRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final InterviewMBTIRepository interviewMBTIRepository;
    private final VoiceAnalysisRepository voiceAnalysisRepository;
    private final FacialAnalysisRepository facialAnalysisRepository;

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupDeletedUsers() {
        log.info("⏰ 탈퇴 유저 정리 스케줄러 시작");

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        
        List<User> usersToDelete = userRepository.findAll().stream()
            .filter(u -> u.getIsDeleted() == 1)
            .filter(u -> u.getDeletedAt() != null)
            .filter(u -> u.getDeletedAt().isBefore(thirtyDaysAgo))
            .toList();

        if (usersToDelete.isEmpty()) {
            log.info("✅ 정리할 탈퇴 유저 없음");
            return;
        }

        log.info("🗑️ {} 명의 탈퇴 유저를 완전 삭제합니다", usersToDelete.size());

        for (User user : usersToDelete) {
            try {
                hardDeleteUser(user);
                log.info("✅ 유저 완전 삭제 완료: id={}, email={}, 탈퇴일={}", 
                    user.getId(), user.getEmail(), user.getDeletedAt());
            } catch (Exception e) {
                log.error("❌ 유저 삭제 실패: id={}, error={}", user.getId(), e.getMessage(), e);
            }
        }

        log.info("⏰ 탈퇴 유저 정리 완료");
    }

    private void hardDeleteUser(User user) {
        Long userId = user.getId();
        
        log.debug("하드 삭제 시작: userId={}", userId);

        try {
            answerRepository.findAll().stream()
                .filter(a -> a.getUser().getId().equals(userId))
                .forEach(answer -> {
                    Long answerId = answer.getId();
                    voiceAnalysisRepository.deleteAll(
                        voiceAnalysisRepository.findAll().stream()
                            .filter(va -> va.getAnswer().getId().equals(answerId))
                            .toList()
                    );
                    facialAnalysisRepository.deleteAll(
                        facialAnalysisRepository.findAll().stream()
                            .filter(fa -> fa.getAnswer().getId().equals(answerId))
                            .toList()
                    );
                    reviewRepository.deleteAll(
                        reviewRepository.findAll().stream()
                            .filter(r -> r.getAnswer() != null && r.getAnswer().getId().equals(answerId))
                            .toList()
                    );
                    feedbackRepository.deleteAll(
                        feedbackRepository.findAll().stream()
                            .filter(f -> f.getAnswer().getId().equals(answerId))
                            .toList()
                    );
                });

            interviewMBTIRepository.deleteAll(
                interviewMBTIRepository.findAll().stream()
                    .filter(mbti -> mbti.getUser() != null && mbti.getUser().getId().equals(userId))
                    .toList()
            );

            reviewRepository.deleteAll(
                reviewRepository.findAll().stream()
                    .filter(r -> r.getReviewer() != null && r.getReviewer().getId().equals(userId))
                    .toList()
            );

            feedbackRepository.deleteAll(
                feedbackRepository.findAll().stream()
                    .filter(f -> f.getReviewer() != null && f.getReviewer().getId().equals(userId))
                    .toList()
            );

            answerRepository.deleteAll(
                answerRepository.findAll().stream()
                    .filter(a -> a.getUser().getId().equals(userId))
                    .toList()
            );

            questionRepository.deleteAll(
                questionRepository.findAll().stream()
                    .filter(q -> q.getQuestioner() != null && q.getQuestioner().getId().equals(userId))
                    .toList()
            );

            sessionRepository.findAll().stream()
                .filter(s -> s.getHost().getId().equals(userId))
                .forEach(session -> {
                    questionRepository.deleteAll(
                        questionRepository.findAll().stream()
                            .filter(q -> q.getSession().getId().equals(session.getId()))
                            .toList()
                    );
                });

            sessionRepository.deleteAll(
                sessionRepository.findAll().stream()
                    .filter(s -> s.getHost().getId().equals(userId))
                    .toList()
            );

            subscriptionRepository.deleteAll(
                subscriptionRepository.findAll().stream()
                    .filter(s -> s.getUser().getId().equals(userId))
                    .toList()
            );

            paymentRepository.deleteAll(
                paymentRepository.findAll().stream()
                    .filter(p -> p.getUser().getId().equals(userId))
                    .toList()
            );

            pushSubscriptionRepository.deleteAll(
                pushSubscriptionRepository.findAll().stream()
                    .filter(ps -> ps.getUser().getId().equals(userId))
                    .toList()
            );

            userRepository.delete(user);
            
        } catch (Exception e) {
            log.error("하드 삭제 중 에러 발생: userId={}", userId, e);
            throw e;
        }
    }
}
