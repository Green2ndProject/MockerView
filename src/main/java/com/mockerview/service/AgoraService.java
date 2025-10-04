package com.mockerview.service;

import com.mockerview.dto.AgoraTokenDTO;
import io.agora.media.RtcTokenBuilder;
import io.agora.media.RtcTokenBuilder.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
public class AgoraService {

    @Value("${agora.app-id}")
    private String appId;

    @Value("${agora.app-certificate}")
    private String appCertificate;

    @Value("${agora.token-expiration:3600}")
    private int tokenExpiration;

    @PostConstruct
    public void init() {
        System.out.println("=== Agora Service 초기화 ===");
        System.out.println("App ID: " + appId);
        System.out.println("App Certificate: " + (appCertificate != null ? "설정됨" : "NULL"));
    }

    public AgoraTokenDTO generateToken(String channelName, Integer uid) {
        int timestamp = (int)(System.currentTimeMillis() / 1000);
        int privilegeExpiredTs = timestamp + tokenExpiration;
        
        RtcTokenBuilder token = new RtcTokenBuilder();
        String result = token.buildTokenWithUid(
            appId, 
            appCertificate, 
            channelName, 
            uid, 
            Role.Role_Publisher, 
            privilegeExpiredTs
        );

        return AgoraTokenDTO.builder()
            .token(result)
            .channel(channelName)
            .appId(appId)
            .uid(uid)
            .expireTime((long) privilegeExpiredTs)
            .build();
    }

    public AgoraTokenDTO generateTokenForSession(Long sessionId, Long userId) {
        String channelName = "session_" + sessionId;
        Integer uid = userId.intValue();
        return generateToken(channelName, uid);
    }
}