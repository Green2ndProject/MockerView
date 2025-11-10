package com.mockerview.service.notification;

import com.mockerview.entity.BadgeType;
import com.mockerview.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BadgeNotification {

    public void sendBadgeNotification(User user, BadgeType badgeType) {
        String message = String.format("%s %s 배지를 획득했습니다! %s",
                badgeType.getEmoji(),
                badgeType.getDisplayName(),
                badgeType.getDescription()
        );

        log.info("🎖️ 배지 획득 알림: {} - {}", user.getUsername(), badgeType.getDisplayName());
    }
}
