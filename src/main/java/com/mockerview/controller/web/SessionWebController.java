package com.mockerview.controller.web;

import com.mockerview.entity.Session;
import com.mockerview.entity.User;
import com.mockerview.entity.Question;
import com.mockerview.entity.Answer;
import com.mockerview.repository.UserRepository;
import com.mockerview.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/session")
@RequiredArgsConstructor
@Slf4j
public class SessionWebController {
    
    private final SessionService sessionService;
    private final UserRepository userRepository;

    @GetMapping("/{sessionId}")
    public String sessionRoom(@PathVariable Long sessionId, 
                            @RequestParam(defaultValue = "1") Long userId,
                            @RequestParam(defaultValue = "테스트사용자") String userName,
                            Model model,
                            HttpSession httpSession) {
        
        try {
            log.info("Loading session room - sessionId: {}, userId: {}, userName: {}", sessionId, userId, userName);
            
            Session session = sessionService.findById(sessionId);
            if (session == null) {
                model.addAttribute("error", "세션을 찾을 수 없습니다.");
                return "error";
            }
            
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                // 테스트 사용자 생성
                user = User.builder()
                    .id(userId)
                    .username("testuser" + userId)
                    .name(userName)
                    .email("test@example.com")
                    .role(User.UserRole.STUDENT)
                    .build();
            }
            
            model.addAttribute("sessionId", sessionId);
            model.addAttribute("sessionTitle", session.getTitle() != null ? session.getTitle() : "모의면접 세션");
            model.addAttribute("userId", userId);
            model.addAttribute("userName", userName);
            model.addAttribute("currentUser", user);
            model.addAttribute("isHost", session.getHost() != null && session.getHost().getId().equals(userId));
            
            log.info("Session room loaded successfully for session: {}", sessionId);
            return "session/session";
            
        } catch (Exception e) {
            log.error("Error loading session room - sessionId: {}, userId: {}: ", sessionId, userId, e);
            model.addAttribute("error", "세션을 불러올 수 없습니다: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/list")
    public String sessionList(Model model, HttpSession httpSession) {
        try {
            log.info("Loading session list...");
            
            List<Session> sessions = sessionService.getAllSessions();
            model.addAttribute("sessions", sessions);
            
            Long userId = (Long) httpSession.getAttribute("userId");
            String userName = (String) httpSession.getAttribute("userName");
            
            if (userId != null && userName != null) {
                User currentUser = userRepository.findById(userId).orElse(null);
                model.addAttribute("currentUser", currentUser);
            }
            
            log.info("Session list loaded successfully");
            return "session/list";
            
        } catch (Exception e) {
            log.error("Error loading session list: ", e);
            model.addAttribute("error", "세션 목록을 불러올 수 없습니다: " + e.getMessage());
            return "session/list";
        }
    }
}
