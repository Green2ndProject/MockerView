package com.mockerview.controller.web;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.Answer;
import com.mockerview.entity.Question;
import com.mockerview.entity.Session;
import com.mockerview.entity.User;
import com.mockerview.entity.User.UserRole;
import com.mockerview.repository.UserRepository;
import com.mockerview.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/session")
@RequiredArgsConstructor
@Slf4j
public class SessionWebController {
    
    private final SessionService sessionService;
    private final UserRepository userRepository;

    @GetMapping("/{sessionId}/join")
    public String joinSession(@PathVariable Long sessionId,
                                @RequestParam String role,
                                HttpSession httpSession) {
        
        Long userId = (Long) httpSession.getAttribute("userId");
        
        if (userId == null) {
            log.warn("비로그인 사용자 세션 접근 시도");
            return "redirect:/auth/login";
        }
        
        User.UserRole selectedRole;
        try {
            selectedRole = User.UserRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            selectedRole = User.UserRole.STUDENT;
        }
        
        httpSession.setAttribute("sessionRole", selectedRole);
        log.info("세션 역할 설정 - userId: {}, role: {}", userId, selectedRole);
        
        return "redirect:/session/" + sessionId;
    }

    @GetMapping("/{sessionId}")
    public String sessionRoom(@PathVariable Long sessionId, 
                            Model model,
                            HttpSession httpSession) {
        
        Long userId = (Long) httpSession.getAttribute("userId");
        String userName = (String) httpSession.getAttribute("userName");
        User.UserRole sessionRole = (User.UserRole) httpSession.getAttribute("sessionRole");
        
        if (userId == null) {
            log.warn("비로그인 사용자 세션 접근 시도");
            return "redirect:/auth/login";
        }
        
        if (sessionRole == null) {
            sessionRole = User.UserRole.STUDENT;
        }
        
        try {
            log.info("세션 접속 - sessionId: {}, userId: {}, userName: {}, role: {}", 
                sessionId, userId, userName, sessionRole);
            
            Session session = sessionService.findById(sessionId);
            if (session == null) {
                model.addAttribute("error", "세션을 찾을 수 없습니다.");
                return "error";
            }
            
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
            
            boolean isHost = sessionRole.equals(User.UserRole.HOST);
            
            model.addAttribute("sessionId", sessionId);
            model.addAttribute("sessionTitle", session.getTitle() != null ? session.getTitle() : "모의면접 세션");
            model.addAttribute("userId", userId);
            model.addAttribute("userName", user.getName());
            model.addAttribute("currentUser", user);
            model.addAttribute("isHost", isHost);
            
            log.info("세션 로드 완료 - 사용자: {}, 역할: {}, 호스트여부: {}", 
                user.getName(), sessionRole, isHost);
            
            return "session/session";
            
        } catch (Exception e) {
            log.error("세션 로드 오류 - sessionId: {}, userId: {}: ", sessionId, userId, e);
            model.addAttribute("error", "세션을 불러올 수 없습니다: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/list")
    public String sessionList(Model model, HttpSession httpSession) {
        try {
            log.info("세션 목록 로드 중...");
            
            List<Session> sessions = sessionService.getAllSessions();
            model.addAttribute("sessions", sessions);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if(authentication != null && authentication.getPrincipal() instanceof CustomUserDetails){

                CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

                String username = customUserDetails.getUsername();

                String userRoleString = customUserDetails.getAuthorities().stream()
                .findFirst() 
                .map(a -> a.getAuthority().replace("ROLE_", "")) 
                .orElse(null);

                model.addAttribute("currentUser", username);
                model.addAttribute("isLoggedIn", true);
                model.addAttribute("userRoleString", userRoleString);

                log.info("세션 목록 로드 완료 - {}개 세션. 현재 사용자: {}", sessions.size(), username);
            }else{

                model.addAttribute("currentUser", null);
                model.addAttribute("isLoggedIn", false);
                log.info("세션 목록 로드 완료 - {}개 세션. 비로그인 상태.", sessions.size());
            }
            
            log.info("세션 목록 로드 완료 - {}개 세션", sessions.size());
            return "session/list";
            
        } catch (Exception e) {
            log.error("세션 목록 로드 오류: ", e);
            model.addAttribute("error", "세션 목록을 불러올 수 없습니다: " + e.getMessage());
            return "session/list";
        }
    }

    @PostMapping("/create")
    public String createSession(@RequestParam String title, 
                                HttpSession httpSession) {
        try {
            Long hostId = (Long) httpSession.getAttribute("userId");
            
            if (hostId == null) {
                return "redirect:/auth/login";
            }
            
            log.info("세션 생성 요청 - title: {}, hostId: {}", title, hostId);
            sessionService.createSession(title, hostId);
            log.info("세션 생성 완료");
            return "redirect:/session/list?success=세션이 생성되었습니다";
        } catch (Exception e) {
            log.error("세션 생성 오류: ", e);
            return "redirect:/session/list?error=" + e.getMessage();
        }
    }

    @GetMapping("/detail/{id}")
    public String sessionDetail(@PathVariable Long id, Model model, HttpSession httpSession) {
        try {
            Session sess = sessionService.findById(id);
            List<Question> questions = sessionService.getSessionQuestions(id);
            List<Answer> answers = sessionService.getSessionAnswers(id);
            
            Map<Long, List<Answer>> answersByQuestion = answers.stream()
                .collect(Collectors.groupingBy(a -> a.getQuestion().getId()));
            
            Long userId = (Long) httpSession.getAttribute("userId");
            User currentUser = userId != null ? userRepository.findById(userId).orElse(null) : null;
            
            int totalAnswerCount = answers.size();
            int answeredQuestionCount = answersByQuestion.size();
            
            model.addAttribute("interviewSession", sess);
            model.addAttribute("questions", questions);
            model.addAttribute("answersByQuestion", answersByQuestion);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("totalAnswerCount", totalAnswerCount);
            model.addAttribute("answeredQuestionCount", answeredQuestionCount);
            
            log.info("세션 상세 로드 완료 - sessionId: {}", id);
            
            return "session/detail";
            
        } catch (Exception e) {
            log.error("세션 상세 조회 오류: ", e);
            return "redirect:/session/list";
        }
    }
}