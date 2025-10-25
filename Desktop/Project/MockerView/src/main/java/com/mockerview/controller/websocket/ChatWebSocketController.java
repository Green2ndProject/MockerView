package com.mockerview.controller.websocket;

import com.mockerview.entity.User;
import com.mockerview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    @MessageMapping("/session/{sessionId}/chat")
    public void handleChat(
            @DestinationVariable Long sessionId,
            @Payload Map<String, Object> message,
            Principal principal
    ) {
        try {
            String username = principal.getName();
            log.info("💬 채팅 수신 - Session: {}, User: {}, Message: {}", 
                sessionId, username, message);

            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            Map<String, Object> chatMessage = new HashMap<>();
            chatMessage.put("sessionId", sessionId);
            chatMessage.put("userId", user.getId());
            chatMessage.put("userName", user.getName());
            chatMessage.put("message", message.get("message"));
            chatMessage.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend(
                "/topic/session/" + sessionId + "/chat",
                chatMessage
            );

            log.info("✅ 채팅 전송 완료: {} - {}", user.getName(), message.get("message"));

        } catch (Exception e) {
            log.error("❌ 채팅 전송 실패", e);
        }
    }
}
