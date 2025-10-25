package com.mockerview.service;

import com.mockerview.dto.AgoraTokenDTO;
import io.agora.media.RtcTokenBuilder2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

@Service
@Slf4j
public class AgoraService {

    @Value("${agora.app-id}")
    private String appId;

    @Value("${agora.app-certificate}")
    private String appCertificate;

    @Value("${agora.token-expiration:3600}")
    private int tokenExpiration;

    @PostConstruct
    public void init() {
        log.info("=== Agora Service 초기화 ===");
        log.info("App ID: {}", appId);
        log.info("App Certificate: {}", appCertificate);
    }

    public AgoraTokenDTO generateToken(String channelName, Integer uid) {
        log.info("=== 토큰 생성 시작 ===");
        log.info("App ID: {}", appId);
        log.info("Channel: {}", channelName);
        log.info("UID: {}", uid);
        
        if (appCertificate == null || appCertificate.trim().isEmpty()) {
            log.warn("⚠️ Certificate 미설정 - 테스트 모드로 작동");
            return AgoraTokenDTO.builder()
                .appId(appId)
                .channel(channelName)
                .token(null)
                .uid(uid)
                .expireTime(null)
                .build();
        }
        
        log.info("App Certificate: {}...", appCertificate.substring(0, Math.min(10, appCertificate.length())));
        
        int timestamp = (int)(System.currentTimeMillis() / 1000);
        int privilegeExpiredTs = timestamp + tokenExpiration;
        
        try {
            RtcTokenBuilder2 tokenBuilder = new RtcTokenBuilder2();
            String generatedToken = tokenBuilder.buildTokenWithUid(
                appId, 
                appCertificate, 
                channelName, 
                uid, 
                RtcTokenBuilder2.Role.ROLE_PUBLISHER, 
                tokenExpiration,
                privilegeExpiredTs
            );
            
            log.info("✅ Agora 토큰 생성 성공!");
            log.info("Token Length: {}", generatedToken.length());
            log.info("Token Preview: {}...", generatedToken.substring(0, Math.min(30, generatedToken.length())));
            log.info("Expire Time: {} ({}초 후)", privilegeExpiredTs, tokenExpiration);

            return AgoraTokenDTO.builder()
                .appId(appId)
                .channel(channelName)
                .token(generatedToken)
                .uid(uid)
                .expireTime((long) privilegeExpiredTs)
                .build();
        } catch (Exception e) {
            log.error("❌ Agora 토큰 생성 실패!", e);
            throw new RuntimeException("토큰 생성 실패: " + e.getMessage(), e);
        }
    }

    public AgoraTokenDTO generateTokenForSession(Long sessionId, Long userId) {
        String channelName = "session_" + sessionId;
        
        Integer uid = userId.intValue();
        
        log.info("세션 토큰 생성 - sessionId: {}, userId: {}, UID: {}, channel: {}", 
            sessionId, userId, uid, channelName);
        
        return generateToken(channelName, uid);
    }
}