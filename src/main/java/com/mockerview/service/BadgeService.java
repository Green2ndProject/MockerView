package com.mockerview.service;

import com.mockerview.entity.*;
import com.mockerview.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeService {

    private final UserBadgeRepository userBadgeRepository;
    private final SessionRepository sessionRepository;
    private final InterviewReportRepository reportRepository;
    private final NotificationService notificationService;

    @Transactional
    public List<UserBadge> checkAndAwardBadges(User user, Session session) {
        log.info("🏆 배지 체크 시작: User {}, Session {}", user.getId(), session.getId());

        List<UserBadge> newBadges = new ArrayList<>();

        checkFirstInterview(user, newBadges);
        checkEarlyBird(user, session, newBadges);
        checkNightOwl(user, session, newBadges);
        checkPerfectScore(user, session, newBadges);
        checkInterviewMilestones(user, newBadges);

        if (!newBadges.isEmpty()) {
            log.info("✅ 새로운 배지 획득: {} 개", newBadges.size());
            newBadges.forEach(badge -> {
                notificationService.sendBadgeNotification(user, badge.getBadgeType());
            });
        }

        return newBadges;
    }

    private void checkFirstInterview(User user, List<UserBadge> newBadges) {
        if (hasBadge(user, BadgeType.FIRST_INTERVIEW)) return;

        long count = sessionRepository.countByUserAndSessionStatus(user, Session.SessionStatus.ENDED);
        if (count == 1) {
            awardBadge(user, BadgeType.FIRST_INTERVIEW, newBadges);
        }
    }

    private void checkEarlyBird(User user, Session session, List<UserBadge> newBadges) {
        if (hasBadge(user, BadgeType.EARLY_BIRD)) return;

        LocalTime sessionTime = session.getCreatedAt().toLocalTime();
        if (sessionTime.isBefore(LocalTime.of(7, 0))) {
            awardBadge(user, BadgeType.EARLY_BIRD, newBadges);
        }
    }

    private void checkNightOwl(User user, Session session, List<UserBadge> newBadges) {
        if (hasBadge(user, BadgeType.NIGHT_OWL)) return;

        LocalTime sessionTime = session.getCreatedAt().toLocalTime();
        if (sessionTime.isAfter(LocalTime.of(23, 0))) {
            awardBadge(user, BadgeType.NIGHT_OWL, newBadges);
        }
    }

    private void checkPerfectScore(User user, Session session, List<UserBadge> newBadges) {
        if (hasBadge(user, BadgeType.PERFECT_SCORE)) return;

        Optional<InterviewReport> reportOpt = reportRepository.findBySession(session);
        if (reportOpt.isPresent() && reportOpt.get().getOverallScore() >= 90) {
            awardBadge(user, BadgeType.PERFECT_SCORE, newBadges);
        }
    }

    private void checkInterviewMilestones(User user, List<UserBadge> newBadges) {
        long count = sessionRepository.countByUserAndSessionStatus(user, Session.SessionStatus.ENDED);

        if (count >= 100 && !hasBadge(user, BadgeType.INTERVIEW_100)) {
            awardBadge(user, BadgeType.INTERVIEW_100, newBadges);
        } else if (count >= 50 && !hasBadge(user, BadgeType.INTERVIEW_50)) {
            awardBadge(user, BadgeType.INTERVIEW_50, newBadges);
        } else if (count >= 10 && !hasBadge(user, BadgeType.INTERVIEW_10)) {
            awardBadge(user, BadgeType.INTERVIEW_10, newBadges);
        }
    }

    private boolean hasBadge(User user, BadgeType badgeType) {
        return userBadgeRepository.existsByUserAndBadgeType(user, badgeType);
    }

    private void awardBadge(User user, BadgeType badgeType, List<UserBadge> newBadges) {
        UserBadge badge = UserBadge.builder()
                .user(user)
                .badgeType(badgeType)
                .build();

        badge = userBadgeRepository.save(badge);
        newBadges.add(badge);

        log.info("🎖️ 배지 획득: {} - {}", user.getUsername(), badgeType.getDisplayName());
    }

    @Transactional(readOnly = true)
    public List<UserBadge> getUserBadges(User user) {
        return userBadgeRepository.findByUserOrderByEarnedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getBadgeProgress(User user) {
        Map<String, Object> progress = new HashMap<>();

        long totalBadges = BadgeType.values().length;
        long earnedBadges = userBadgeRepository.countByUser(user);
        progress.put("total", totalBadges);
        progress.put("earned", earnedBadges);
        progress.put("percentage", (earnedBadges * 100.0) / totalBadges);

        List<UserBadge> badges = userBadgeRepository.findByUserOrderByEarnedAtDesc(user);
        progress.put("badges", badges);

        long interviewCount = sessionRepository.countByUserAndSessionStatus(user, Session.SessionStatus.ENDED);
        progress.put("interviewCount", interviewCount);
        progress.put("nextMilestone", getNextMilestone(interviewCount));

        return progress;
    }

    private int getNextMilestone(long current) {
        if (current < 10) return 10;
        if (current < 50) return 50;
        if (current < 100) return 100;
        return 200;
    }
}
