package com.mockerview.controller.api;

import com.mockerview.entity.RefreshToken;
import com.mockerview.jwt.JWTUtil;
import com.mockerview.repository.UserRepository;
import com.mockerview.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class TokenRefreshController {
    
    private final RefreshTokenService refreshTokenService;
    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;
    
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        String refreshToken = null;
        
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("RefreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }
        
        if (refreshToken == null) {
            log.warn("⚠️ Refresh token not found in cookies");
            return ResponseEntity.status(401).body("{\"success\":false,\"message\":\"Refresh token missing\"}");
        }
        
        Optional<RefreshToken> tokenOptional = refreshTokenService.validateRefreshToken(refreshToken);
        
        if (tokenOptional.isEmpty()) {
            log.warn("⚠️ Invalid or expired refresh token");
            
            Cookie deleteCookie = new Cookie("Authorization", null);
            deleteCookie.setMaxAge(0);
            deleteCookie.setPath("/");
            response.addCookie(deleteCookie);
            
            Cookie deleteRefreshCookie = new Cookie("RefreshToken", null);
            deleteRefreshCookie.setMaxAge(0);
            deleteRefreshCookie.setPath("/");
            response.addCookie(deleteRefreshCookie);
            
            return ResponseEntity.status(401).body("{\"success\":false,\"message\":\"Invalid refresh token\"}");
        }
        
        RefreshToken token = tokenOptional.get();
        String username = token.getUsername();
        
        String role = userRepository.findByUsername(username)
            .map(user -> user.getRole().toString())
            .orElse("ROLE_USER");
        
        String newAccessToken = jwtUtil.createJwt(username, role, 7 * 24 * 60 * 60 * 1000L);
        
        Cookie accessCookie = new Cookie("Authorization", newAccessToken);
        accessCookie.setMaxAge(7 * 24 * 60 * 60);
        accessCookie.setPath("/");
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false);
        
        response.addCookie(accessCookie);
        
        log.info("✅ Token refreshed for user: {}", username);
        
        return ResponseEntity.ok().body("{\"success\":true,\"message\":\"Token refreshed\"}");
    }
}