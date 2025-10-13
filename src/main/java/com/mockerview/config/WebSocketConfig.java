package com.mockerview.config;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.jwt.JWTUtil;
import com.mockerview.entity.User;
import com.mockerview.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private JWTUtil jwtUtil;
    
    @Autowired
    private UserRepository userRepository;

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{25000, 25000})
                .setTaskScheduler(taskScheduler());
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(128 * 1024)
                .setSendBufferSizeLimit(512 * 1024)
                .setSendTimeLimit(20000);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    Principal principal = accessor.getUser();
                    
                    if (principal instanceof UsernamePasswordAuthenticationToken) {
                        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) principal;
                        Object details = auth.getDetails();
                        
                        if (details instanceof CustomUserDetails) {
                            CustomUserDetails userDetails = (CustomUserDetails) details;
                            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                            
                            if (sessionAttributes != null) {
                                sessionAttributes.put("userId", userDetails.getUserId());
                                sessionAttributes.put("userName", userDetails.getName());
                                sessionAttributes.put("userRole", userDetails.getAuthorities().iterator().next().getAuthority());
                                
                                System.out.println("✅ WebSocket 인증 성공: " + userDetails.getName() + " (ID: " + userDetails.getUserId() + ")");
                            }
                        }
                    }
                }
                
                return message;
            }
        });
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(new DefaultHandshakeHandler() {
                    @Override
                    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
                        System.out.println("🔍 determineUser 호출됨");
                        
                        if (!(request instanceof ServletServerHttpRequest)) {
                            System.out.println("❌ ServletServerHttpRequest가 아님");
                            return null;
                        }
                        
                        ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                        HttpServletRequest httpRequest = servletRequest.getServletRequest();
                        
                        String token = extractTokenFromRequest(httpRequest);
                        
                        if (token == null) {
                            System.out.println("❌ 토큰을 찾을 수 없음");
                            return null;
                        }
                        
                        System.out.println("🔑 토큰 발견: " + token.substring(0, Math.min(20, token.length())) + "...");
                        
                        try {
                            if (jwtUtil.isExpired(token)) {
                                System.out.println("❌ 토큰 만료됨");
                                return null;
                            }
                            
                            String username = jwtUtil.getUsername(token);
                            User user = userRepository.findByUsername(username).orElse(null);
                            
                            if (user == null) {
                                System.out.println("❌ 사용자를 찾을 수 없음: " + username);
                                return null;
                            }
                            
                            System.out.println("🤝 Handshake 인증 성공: " + user.getName() + " (ID: " + user.getId() + ")");
                            
                            CustomUserDetails userDetails = new CustomUserDetails(user);
                            
                            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
                            );
                            auth.setDetails(userDetails);
                            
                            return auth;
                            
                        } catch (Exception e) {
                            System.err.println("❌ Handshake 인증 실패: " + e.getMessage());
                            e.printStackTrace();
                            return null;
                        }
                    }
                    
                    private String extractTokenFromRequest(HttpServletRequest request) {
                        String tokenParam = request.getParameter("token");
                        if (tokenParam != null && !tokenParam.isEmpty()) {
                            System.out.println("🔑 URL 파라미터에서 토큰 발견");
                            return tokenParam;
                        }
                        
                        String authHeader = request.getHeader("Authorization");
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            System.out.println("🔑 Authorization 헤더에서 토큰 발견");
                            return authHeader.substring(7);
                        }
                        
                        Cookie[] cookies = request.getCookies();
                        if (cookies != null) {
                            for (Cookie cookie : cookies) {
                                if ("Authorization".equals(cookie.getName())) {
                                    System.out.println("🍪 Cookie에서 토큰 발견");
                                    return cookie.getValue();
                                }
                            }
                        }
                        
                        System.out.println("❌ 토큰을 찾을 수 없음 (파라미터, 헤더, 쿠키 모두)");
                        return null;
                    }
                })
                .withSockJS()
                .setHeartbeatTime(25000)
                .setDisconnectDelay(5000)
                .setSessionCookieNeeded(false);
    }
}