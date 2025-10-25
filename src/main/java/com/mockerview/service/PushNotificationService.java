package com.mockerview.service;

import com.mockerview.dto.PushSubscriptionDTO;
import com.mockerview.entity.PushSubscription;
import com.mockerview.entity.User;
import com.mockerview.repository.PushSubscriptionRepository;
import com.mockerview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {
    
    private final PushSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    
    @Value("${vapid.public.key}")
    private String publicKey;
    
    @Value("${vapid.private.key}")
    private String privateKey;
    
    @Value("${vapid.subject}")
    private String subject;
    
    private PushService pushService;
    
    @PostConstruct
    public void init() throws GeneralSecurityException {
        Security.addProvider(new BouncyCastleProvider());
        pushService = new PushService(publicKey, privateKey, subject);
        log.info("✅ Push Notification Service initialized");
    }
    
    @Transactional
    public void subscribe(String username, PushSubscriptionDTO dto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        PushSubscription subscription = subscriptionRepository
                .findByEndpoint(dto.getEndpoint())
                .orElse(PushSubscription.builder()
                        .user(user)
                        .endpoint(dto.getEndpoint())
                        .build());
        
        subscription.setP256dh(dto.getKeys().getP256dh());
        subscription.setAuth(dto.getKeys().getAuth());
        subscription.setActive(true);
        
        subscriptionRepository.save(subscription);
        log.info("✅ Push subscription saved for user: {}", username);
    }
    
    @Transactional
    public void unsubscribe(String endpoint) {
        subscriptionRepository.deleteByEndpoint(endpoint);
        log.info("🗑️ Push subscription removed: {}", endpoint);
    }
    
    public void sendNotification(User user, String title, String body, String url) {
        List<PushSubscription> subscriptions = subscriptionRepository.findByUserAndActiveTrue(user);
        
        String payload = String.format(
            "{\"title\":\"%s\",\"body\":\"%s\",\"url\":\"%s\",\"icon\":\"/images/192.png\"}",
            title, body, url
        );
        
        for (PushSubscription sub : subscriptions) {
            try {
                Notification notification = new Notification(
                    sub.getEndpoint(),
                    sub.getP256dh(),
                    sub.getAuth(),
                    payload
                );
                
                HttpResponse response = pushService.send(notification);
                int statusCode = response.getStatusLine().getStatusCode();
                
                if (statusCode == 201) {
                    log.info("✅ Push sent successfully");
                } else if (statusCode == 410) {
                    log.warn("⚠️ Subscription expired, removing");
                    sub.setActive(false);
                    subscriptionRepository.save(sub);
                } else {
                    log.error("❌ Push failed with status: {}", statusCode);
                }
                
            } catch (GeneralSecurityException | IOException | JoseException | ExecutionException | InterruptedException e) {
                log.error("❌ Error sending push notification", e);
            }
        }
    }
    
    public void notifySessionStart(User user, Long sessionId, String sessionTitle) {
        sendNotification(
            user,
            "🎯 세션이 시작되었습니다!",
            sessionTitle + " - 지금 참여하세요",
            "/session/" + sessionId
        );
    }
    
    public void notifyFeedbackReceived(User user, Long sessionId) {
        sendNotification(
            user,
            "💬 새로운 피드백",
            "면접에 대한 피드백이 도착했습니다",
            "/session/" + sessionId + "/scoreboard"
        );
    }
}
