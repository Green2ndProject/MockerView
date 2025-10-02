package com.mockerview.jwt;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JWTLogoutHandler implements LogoutHandler {

    private static final String JWT_COOKIE_NAME = "Authorization"; 

    @Override
    public void logout(HttpServletRequest request, 
                       HttpServletResponse response, 
                       Authentication authentication) {
        
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, null); 
        
        cookie.setMaxAge(0);

        cookie.setPath("/");

        cookie.setHttpOnly(false);

        response.addCookie(cookie);

        System.out.println("로그아웃 처리됨: JWT Cookie '" + JWT_COOKIE_NAME + "' 삭제 요청 전송.");
    }

    
}
