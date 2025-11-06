package com.mockerview.scheduler;

import com.mockerview.entity.User;
import com.mockerview.repository.UserRepository;
import com.mockerview.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InactivityNotificationScheduler {
    
    private final UserRepository userRepository;
    private final PushNotificationService pushService;
    
    @Scheduled(cron = "0 0 10 * * ?")
    public void sendInactivityNotifications() {
        log.info("🔔 Checking for inactive users...");
        
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        
        List<User> users = userRepository.findAll();
        
        for (User user : users) {
            if (user.getLastLoginDate() == null) continue;
            
            LocalDateTime lastLogin = user.getLastLoginDate();
            
            if (lastLogin.isBefore(sevenDaysAgo)) {
                pushService.sendNotification(
                    user,
                    "🎯 면접 준비를 멈추지 마세요!",
                    "일주일이 지났어요. 오늘 하루 10분만 투자해보세요!",
                    "/session/list"
                );
                log.info("📤 7-day inactivity notification sent to: {}", user.getUsername());
                
            } else if (lastLogin.isBefore(threeDaysAgo)) {
                pushService.sendNotification(
                    user,
                    "💪 다시 연습해볼까요?",
                    "3일 동안 접속하지 않으셨어요. 면접 준비를 이어가세요!",
                    "/session/list"
                );
                log.info("📤 3-day inactivity notification sent to: {}", user.getUsername());
            }
        }
        
        log.info("✅ Inactivity notification check completed");
    }
}
