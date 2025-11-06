package com.mockerview.service;

import com.mockerview.entity.RefreshToken;
import com.mockerview.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    private static final long REFRESH_TOKEN_VALIDITY = 30L * 24 * 60 * 60 * 1000;
    
    @Transactional
    public String createRefreshToken(String username) {
        refreshTokenRepository.deleteByUsername(username);
        
        String token = UUID.randomUUID().toString();
        
        RefreshToken refreshToken = RefreshToken.builder()
            .token(token)
            .username(username)
            .expiryDate(LocalDateTime.now().plusDays(30))
            .createdAt(LocalDateTime.now())
            .lastUsedAt(LocalDateTime.now())
            .build();
        
        refreshTokenRepository.save(refreshToken);
        log.info("✅ Refresh token created for user: {}", username);
        
        return token;
    }
    
    @Transactional
    public Optional<RefreshToken> validateRefreshToken(String token) {
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByToken(token);
        
        if (refreshToken.isEmpty()) {
            log.warn("⚠️ Refresh token not found");
            return Optional.empty();
        }
        
        if (refreshToken.get().isExpired()) {
            log.warn("⚠️ Refresh token expired");
            refreshTokenRepository.delete(refreshToken.get());
            return Optional.empty();
        }
        
        refreshToken.get().setLastUsedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken.get());
        
        return refreshToken;
    }
    
    @Transactional
    public void deleteRefreshToken(String username) {
        refreshTokenRepository.deleteByUsername(username);
        log.info("🗑️ Refresh token deleted for user: {}", username);
    }
    
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
        log.info("🧹 Expired refresh tokens cleaned up");
    }
}
