package com.mydiet.controller;

import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final String ADMIN_PASSWORD = "oicrcutie";
    private static final String ADMIN_SESSION_KEY = "admin_logged_in";

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request, HttpSession session) {
        String password = request.get("password");
        
        if (ADMIN_PASSWORD.equals(password)) {
            session.setAttribute(ADMIN_SESSION_KEY, true);
            return Map.of("success", true, "message", "로그인 성공");
        } else {
            return Map.of("success", false, "message", "비밀번호가 올바르지 않습니다");
        }
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        session.removeAttribute(ADMIN_SESSION_KEY);
        return Map.of("success", true, "message", "로그아웃 되었습니다");
    }

    @GetMapping("/check")
    public Map<String, Object> checkAuth(HttpSession session) {
        boolean isLoggedIn = Boolean.TRUE.equals(session.getAttribute(ADMIN_SESSION_KEY));
        return Map.of("authenticated", isLoggedIn);
    }
}
