package com.mockerview.service;

import com.mockerview.dto.AgoraTokenDTO;
import io.agora.media.RtcTokenBuilder;
import io.agora.media.RtcTokenBuilder.Role;
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
        log.info("App Certificate: {}", (appCertificate != null && !appCertificate.isEmpty()) ? "설정됨" : "NULL");
    }

    public AgoraTokenDTO generateToken(String channelName, Integer uid) {
        int timestamp = (int)(System.currentTimeMillis() / 1000);
        int privilegeExpiredTs = timestamp + tokenExpiration;
        
        RtcTokenBuilder tokenBuilder = new RtcTokenBuilder();
        String generatedToken = tokenBuilder.buildTokenWithUid(
            appId, 
            appCertificate, 
            channelName, 
            uid, 
            Role.Role_Publisher, 
            privilegeExpiredTs
        );
        
        log.info("Agora 토큰 생성 완료 - channel: {}, uid: {}, token: {}...", 
            channelName, uid, generatedToken.substring(0, Math.min(20, generatedToken.length())));

        return AgoraTokenDTO.builder()
            .appId(appId)
            .channel(channelName)
            .token(generatedToken)
            .uid(uid)
            .expireTime((long) privilegeExpiredTs)
            .build();
    }

    public AgoraTokenDTO generateTokenForSession(Long sessionId, Long userId) {
        String channelName = "session_" + sessionId;
        
        // UID 충돌 방지: userId + 타임스탬프
        // userId를 10000배 + 현재 밀리초의 마지막 4자리
        Integer uid = (int) (userId * 10000 + (System.currentTimeMillis() % 10000));
        
        log.info("세션 토큰 생성 - sessionId: {}, userId: {}, generated UID: {}, channel: {}", 
            sessionId, userId, uid, channelName);
        
        return generateToken(channelName, uid);
    }
}