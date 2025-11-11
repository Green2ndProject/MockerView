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

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    
    @Transactional
    public String createRefreshToken(String username) {
        refreshTokenRepository.deleteByUsername(username);
        
        String token = UUID.randomUUID().toString();
        
        RefreshToken refreshToken = RefreshToken.builder()
            .token(token)
            .username(username)
            .expiresAt(LocalDateTime.now().plusDays(30))
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
    public void deleteRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
        log.info("✅ Refresh token deleted");
    }
    
    @Transactional
    public void deleteAllUserTokens(String username) {
        refreshTokenRepository.deleteByUsername(username);
        log.info("✅ All refresh tokens deleted for user: {}", username);
    }
}
