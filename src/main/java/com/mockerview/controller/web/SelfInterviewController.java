package com.mockerview.controller.web;

import com.mockerview.entity.User;
import com.mockerview.entity.Session;
import com.mockerview.service.SelfInterviewService;
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
import java.util.stream.Collectors;

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
            
            List<Session> allSessions = sessionRepository.findByHostId(user.getId());
            List<Session> selfSessions = allSessions.stream()
                .filter(s -> "SELF".equals(s.getSessionType()))
                .sorted((s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt()))
                .collect(Collectors.toList());
            
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
            
            Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));
            
            model.addAttribute("session", session);
            
            return "selfinterview/room";
        } catch (Exception e) {
            log.error("셀프면접 룸 조회 오류", e);
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }
}