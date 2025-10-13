package com.mockerview.controller.web;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.User;
import com.mockerview.entity.Session;
import com.mockerview.repository.SessionRepository;
import com.mockerview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/selfinterview")
@RequiredArgsConstructor
@Slf4j
public class SelfInterviewController {
    
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    @GetMapping("/create")
    public String createPage(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/auth/login";
        }
        model.addAttribute("username", authentication.getName());
        return "selfinterview/create";
    }

    @GetMapping("/list")
    public String listPage(Authentication authentication, Model model) {
        try {
            if (authentication == null) {
                return "redirect:/auth/login";
            }
            
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            log.info("=== DEBUG: User ID: {}", user.getId());
            
            List<Session> allSessions = sessionRepository.findByHostId(user.getId());
            log.info("=== DEBUG: Total sessions by host: {}", allSessions.size());
            
            for (Session s : allSessions) {
                log.info("=== DEBUG: Session {} - title: {}, isSelfInterview: {}, sessionType: {}", 
                    s.getId(), s.getTitle(), s.getIsSelfInterview(), s.getSessionType());
            }
            
            List<Session> selfSessions = sessionRepository.findSelfInterviewsByHostId(user.getId());
            log.info("=== DEBUG: Self interview sessions: {}", selfSessions.size());
            
            for (Session s : selfSessions) {
                log.info("=== DEBUG: Self Session {} - title: {}", s.getId(), s.getTitle());
            }
            
            model.addAttribute("username", username);
            model.addAttribute("sessions", selfSessions);
            
            return "selfinterview/list";
        } catch (Exception e) {
            log.error("셀프면접 목록 조회 오류", e);
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    @GetMapping("/room/{id}")
    public String roomPage(@PathVariable Long id, Authentication authentication, Model model) {
        try {
            if (authentication == null) {
                return "redirect:/auth/login";
            }
            
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            
            Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));
            
            if (!session.getHost().getId().equals(userDetails.getUserId())) {
                log.warn("Unauthorized access attempt - sessionId: {}, userId: {}", id, userDetails.getUserId());
                return "redirect:/selfinterview/list";
            }
            
            model.addAttribute("session", session);
            
            return "selfinterview/room";
        } catch (Exception e) {
            log.error("셀프면접 룸 조회 오류", e);
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    @GetMapping("/history")
    public String historyPage(Authentication authentication, Model model) {
        try {
            if (authentication == null) {
                return "redirect:/auth/login";
            }
            
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            
            List<Session> selfSessions = sessionRepository.findSelfInterviewsByHostId(userDetails.getUserId());
            
            model.addAttribute("sessions", selfSessions);
            return "selfinterview/history";
        } catch (Exception e) {
            log.error("셀프면접 히스토리 조회 오류", e);
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }
}