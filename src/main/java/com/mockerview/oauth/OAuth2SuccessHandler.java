package com.mockerview.oauth;

import com.mockerview.jwt.JWTUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;

    @Value("${server.servlet.session.cookie.secure:false}")
    private boolean cookieSecure;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        log.info("✅ OAuth2 로그인 성공: {}", authentication.getName());

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        
        String username = oAuth2User.getUsername();
        String role = oAuth2User.getRole();

        String token = jwtUtil.createJwt(username, role, 3 * 60 * 60 * 1000L);

        boolean isHttps = request.isSecure() || 
                            (request.getHeader("X-Forwarded-Proto") != null && 
                            request.getHeader("X-Forwarded-Proto").equals("https"));

        log.info("🔒 HTTPS: {}, 쿠키 Secure 설정: {}", isHttps, cookieSecure || isHttps);

        Cookie cookie = new Cookie("Authorization", token);
        cookie.setHttpOnly(false);
        cookie.setSecure(cookieSecure || isHttps);
        cookie.setPath("/");
        cookie.setMaxAge(3 * 60 * 60);

        response.addCookie(cookie);
        
        log.info("✅ JWT 토큰 쿠키 설정 완료 - HttpOnly: false (JS 접근 가능)");

        response.sendRedirect("/session/list");
    }
}
