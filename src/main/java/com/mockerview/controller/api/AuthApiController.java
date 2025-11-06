package com.mockerview.controller.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Controller
public class AuthApiController {

    @GetMapping("/auth/logout")
    public String logoutPage(HttpServletResponse response) {
        log.info("🚪 로그아웃 요청 (GET)");

        Cookie cookie = new Cookie("Authorization", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);

        response.addCookie(cookie);

        log.info("✅ 로그아웃 완료 - JWT 토큰 쿠키 삭제");

        return "redirect:/";
    }

    @PostMapping("/api/auth/logout")
    @ResponseBody
    public ResponseEntity<?> logoutApi(HttpServletResponse response) {
        log.info("🚪 로그아웃 요청 (POST)");

        Cookie cookie = new Cookie("Authorization", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);

        response.addCookie(cookie);

        log.info("✅ 로그아웃 완료 - JWT 토큰 쿠키 삭제");

        return ResponseEntity.ok()
            .body(Map.of("message", "로그아웃 되었습니다.", "redirect", "/"));
    }
}
