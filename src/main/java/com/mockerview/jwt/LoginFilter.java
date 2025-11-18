package com.mockerview.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockerview.service.RefreshTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public LoginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil, RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/auth/login", "POST"));
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) 
            throws AuthenticationException {
        
        String username = null;
        String password = null;
        
        String contentType = request.getContentType();
        log.info("📝 Content-Type: {}", contentType);
        
        try {
            if (contentType != null && contentType.contains("application/json")) {
                String body = request.getReader().lines()
                        .reduce("", (accumulator, actual) -> accumulator + actual);
                
                log.info("✅ Raw JSON Body: {}", body);
                
                ObjectMapper mapper = new ObjectMapper();
                Map<String, String> credentials = mapper.readValue(body, Map.class);
                
                username = credentials.get("username");
                password = credentials.get("password");
                
                log.info("✅ JSON 파싱 성공 - username: {}", username);
                
            } else {
                username = request.getParameter("username");
                password = request.getParameter("password");
                
                log.info("✅ Form 파싱 성공 - username: {}", username);
            }
            
            if (username == null || password == null) {
                throw new RuntimeException("Username or password is missing");
            }
            
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(username, password, null);
            
            return authenticationManager.authenticate(authToken);
            
        } catch (IOException e) {
            log.error("❌ 파싱 실패", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, 
                                            FilterChain chain, Authentication authentication) 
            throws IOException {
        
        String username = authentication.getName();
        
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority();
        
        String accessToken = jwtUtil.createJwt(username, role, 7 * 24 * 60 * 60 * 1000L);
        String refreshToken = refreshTokenService.createRefreshToken(username);
        
        Cookie accessCookie = new Cookie("Authorization", accessToken);
        accessCookie.setMaxAge(7 * 24 * 60 * 60);
        accessCookie.setPath("/");
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false);
        
        Cookie refreshCookie = new Cookie("RefreshToken", refreshToken);
        refreshCookie.setMaxAge(30 * 24 * 60 * 60);
        refreshCookie.setPath("/");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        
        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"success\":true,\"redirect\":\"/session/list\"}");
        
        log.info("✅ 로그인 성공 - {}. Access: 7일, Refresh: 30일", username);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, 
                                            AuthenticationException failed) 
            throws IOException {
        
        response.setStatus(401);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"로그인 실패\"}");
        
        log.warn("❌ 로그인 실패");
    }
}