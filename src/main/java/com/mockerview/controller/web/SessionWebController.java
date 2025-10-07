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


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)) {
            return null;
        }
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userRepository.findByUsername(userDetails.getUsername())
                .orElse(null);
    }

    @GetMapping("/{sessionId}/join")
    public String joinSession(@PathVariable Long sessionId,
                                @RequestParam String role,
                                Model model) {
        
        User currentUser = getCurrentUser();
        
        if (currentUser == null) {
            log.warn("비로그인 사용자 세션 접근 시도");
            return "redirect:/auth/login";
        }
        
        User.UserRole selectedRole;
        try {
            selectedRole = User.UserRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            selectedRole = User.UserRole.STUDENT;
        }
        
        model.addAttribute("selectedRole", selectedRole);
        log.info("세션 역할 설정 - userId: {}, role: {}", currentUser.getId(), selectedRole);
        
        return "redirect:/session/" + sessionId + "?role=" + selectedRole.name();
    }

    @GetMapping("/{sessionId}")
    public String sessionRoom(@PathVariable Long sessionId, 
                            @RequestParam(required = false) String role,
                            Model model) {
        
        User currentUser = getCurrentUser();
        
        if (currentUser == null) {
            log.warn("비로그인 사용자 세션 접근 시도");
            return "redirect:/auth/login";
        }
        
        User.UserRole sessionRole = User.UserRole.STUDENT;
        if (role != null) {
            try {
                sessionRole = User.UserRole.valueOf(role);
            } catch (IllegalArgumentException e) {
                log.warn("잘못된 역할 파라미터: {}", role);
            }
        }
        
        try {
            log.info("세션 접속 - sessionId: {}, userId: {}, userName: {}, role: {}", 
                sessionId, currentUser.getId(), currentUser.getName(), sessionRole);
            
            Session session = sessionService.findById(sessionId);
            if (session == null) {
                model.addAttribute("error", "세션을 찾을 수 없습니다.");
                return "error";
            }
            
            boolean isHost = sessionRole.equals(User.UserRole.HOST);
            
            model.addAttribute("sessionId", sessionId);
            model.addAttribute("sessionTitle", session.getTitle() != null ? session.getTitle() : "모의면접 세션");
            model.addAttribute("sessionType", session.getSessionType() != null ? session.getSessionType() : "TEXT");
            model.addAttribute("userId", currentUser.getId());
            model.addAttribute("userName", currentUser.getName());
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("isHost", isHost);
            
            log.info("세션 로드 완료 - 사용자: {}, 역할: {}, 호스트여부: {}, 타입: {}", 
                currentUser.getName(), sessionRole, isHost, session.getSessionType());
            
            return "session/session";
            
        } catch (Exception e) {
            log.error("세션 로드 오류 - sessionId: {}, userId: {}: ", sessionId, currentUser.getId(), e);
            model.addAttribute("error", "세션을 불러올 수 없습니다: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/list")
    public String sessionList(
                            @RequestParam(value = "page", defaultValue = "1") int page,
                            @RequestParam(value = "size", defaultValue = "6") int size,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) String sortBy,
                            @RequestParam(required = false) String sortOrder,
                            Model model) {

        try {
            log.info("세션 목록 로드 중...");

            Page<Session> sessionPage; 

            Sort sort = Sort.by(
            sortOrder != null && sortOrder.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC,
            sortBy != null ? sortBy : "createdAt"
            );

            Pageable pageable = PageRequest.of(page - 1, size, sort);
            
            if (keyword != null || status != null) {
                sessionPage = sessionService.searchSessionsPageable(
                keyword, 
                status, 
                pageable 
            );
            } else {
                sessionPage = sessionService.getSessionsPageable(pageable);
            }

            model.addAttribute("sessions", sessionPage.getContent()); 
            model.addAttribute("totalPages", sessionPage.getTotalPages());
            model.addAttribute("serverCurrentPage", page); 
            model.addAttribute("keyword", keyword);

            User currentUser = getCurrentUser();
            
            if (currentUser != null) {
                model.addAttribute("currentUser", currentUser);
                model.addAttribute("isLoggedIn", true);
                log.info("세션 목록 로드 완료 - {}개 세션. 현재 페이지: {}/{} 사용자: {}", sessionPage.getContent().size(), page, sessionPage.getTotalPages(), currentUser.getUsername());
            } else {
                model.addAttribute("currentUser", null);
                model.addAttribute("isLoggedIn", false);
                log.info("세션 목록 로드 완료 - {}개 세션. 비로그인 상태.", sessionPage.getContent().size());
            }
            
            return "session/list";
            
        } catch (Exception e) {
            log.error("세션 목록 로드 오류: ", e);
            model.addAttribute("error", "세션 목록을 불러올 수 없습니다: " + e.getMessage());
            return "session/list";
        }
    }

    @PostMapping("/create")
    public String createSession(@RequestParam String title,
                                @RequestParam(defaultValue = "TEXT") String sessionType,
                                @RequestParam(required = false) String scheduledStartTime) {
        try {
            User currentUser = getCurrentUser();
            
            if (currentUser == null) {
                return "redirect:/auth/login";
            }
            
            LocalDateTime startTime = null;
            if (scheduledStartTime != null && !scheduledStartTime.isEmpty()) {
                startTime = LocalDateTime.parse(scheduledStartTime);
            }
            
            log.info("세션 생성 요청 - title: {}, hostId: {}, type: {}, scheduled: {}", 
                    title, currentUser.getId(), sessionType, startTime);
            sessionService.createSession(title, currentUser.getId(), sessionType, startTime);
            log.info("세션 생성 완료");

            String successMessage = "세션이 생성되었습니다";
            String encodedMessage = URLEncoder.encode(successMessage, StandardCharsets.UTF_8.toString());

            return "redirect:/session/list?success=" + encodedMessage;

        } catch (Exception e) {
            log.error("세션 생성 오류: ", e);
            return "redirect:/session/list?error=" + e.getMessage();
        }
    }


    @GetMapping("/detail/{id}")
public String sessionDetail(@PathVariable Long id, Model model) {
    try {
        Session sess = sessionService.findById(id);
        
        if (sess == null) {
            model.addAttribute("error", "세션을 찾을 수 없습니다.");
            return "redirect:/session/list";
        }
        
        List<Question> questions = sessionService.getSessionQuestions(id);
        List<Answer> answers = sessionService.getSessionAnswers(id);
        
        Map<Long, List<Answer>> answersByQuestion = answers.stream()
            .collect(Collectors.groupingBy(a -> a.getQuestion().getId()));
        
        User currentUser = getCurrentUser();
        
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
        model.addAttribute("error", "세션 상세 정보를 불러올 수 없습니다: " + e.getMessage());
        return "redirect:/session/list";
    }
}
}