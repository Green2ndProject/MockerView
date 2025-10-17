package com.mockerview.jwt;

import com.mockerview.dto.CustomOAuth2User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        String username = oAuth2User.getUsername();
        String role = oAuth2User.getAuthorities().iterator().next().getAuthority();
        String token = jwtUtil.createJwt(username, role, 1000 * 60 * 60 * 3L);
        
        Cookie authCookie = new Cookie("Authorization", token);
        authCookie.setMaxAge(3 * 60 * 60);
        authCookie.setPath("/");
        authCookie.setHttpOnly(true);
        authCookie.setSecure(false);
        response.addCookie(authCookie);
        
        log.info("OAuth2 로그인 성공: {}", username);
        
        String redirectUrl = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("redirectAfterLogin".equals(cookie.getName())) {
                    redirectUrl = cookie.getValue();
                    Cookie deleteCookie = new Cookie("redirectAfterLogin", null);
                    deleteCookie.setMaxAge(0);
                    deleteCookie.setPath("/");
                    response.addCookie(deleteCookie);
                    break;
                }
            }
        }
        
        if (redirectUrl != null && !redirectUrl.isEmpty()) {
            response.sendRedirect(redirectUrl);
        } else {
            response.sendRedirect("/session/list");
        }
    }
}
