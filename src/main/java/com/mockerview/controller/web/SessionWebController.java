package com.mockerview.controller.web;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.Answer;
import com.mockerview.entity.Feedback;
import com.mockerview.entity.Question;
import com.mockerview.entity.Session;
import com.mockerview.entity.User;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.QuestionRepository;
import com.mockerview.repository.SessionRepository;
import com.mockerview.repository.UserRepository;
import com.mockerview.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/session")
@RequiredArgsConstructor
public class SessionWebController {

    private final SessionService sessionService;
    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
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
            model.addAttribute("session", session);
            
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
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            Model model) {
        
        try {
            log.info("세션 목록 로드 중 - status: {}, keyword: {}", status, keyword);
            
            User currentUser = getCurrentUser();
            String currentUsername = currentUser != null ? currentUser.getUsername() : "guest";
            
            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Session> sessionPage;
            
            if (status != null && !status.isEmpty()) {
                Session.SessionStatus sessionStatus = Session.SessionStatus.valueOf(status);
                if (keyword != null && !keyword.isEmpty()) {
                    sessionPage = sessionRepository.searchSessionsPageable(keyword, sessionStatus, pageable);
                } else {
                    sessionPage = sessionRepository.findByStatusPageable(sessionStatus, pageable);
                }
            } else if (keyword != null && !keyword.isEmpty()) {
                sessionPage = sessionRepository.searchSessionsPageable(keyword, null, pageable);
            } else {
                sessionPage = sessionRepository.findAllSessionsWithHost(pageable);
            }
            
            List<Map<String, Object>> sessionList = sessionPage.getContent().stream()
                .map(session -> {
                    Map<String, Object> sessionMap = new HashMap<>();
                    sessionMap.put("id", session.getId());
                    sessionMap.put("title", session.getTitle());
                    sessionMap.put("sessionType", session.getSessionType());
                    sessionMap.put("sessionStatus", session.getSessionStatus().toString());
                    sessionMap.put("createdAt", session.getCreatedAt());
                    sessionMap.put("expiresAt", session.getExpiresAt());
                    sessionMap.put("startTime", session.getStartTime());
                    sessionMap.put("endTime", session.getEndTime());
                    sessionMap.put("mediaEnabled", session.getMediaEnabled());
                    sessionMap.put("agoraChannel", session.getAgoraChannel());
                    sessionMap.put("isSelfInterview", session.getIsSelfInterview());
                    
                    if (session.getHost() != null) {
                        Map<String, Object> hostMap = new HashMap<>();
                        hostMap.put("id", session.getHost().getId());
                        hostMap.put("name", session.getHost().getName());
                        hostMap.put("username", session.getHost().getUsername());
                        hostMap.put("role", session.getHost().getRole().toString());
                        sessionMap.put("host", hostMap);
                    } else {
                        sessionMap.put("host", null);
                    }
                    
                    return sessionMap;
                })
                .collect(Collectors.toList());
            
            long totalCount = sessionRepository.countNonSelfInterviewSessions();
            long plannedCount = sessionRepository.countBySessionStatusAndIsSelfInterview(Session.SessionStatus.PLANNED, "N");
            long runningCount = sessionRepository.countBySessionStatusAndIsSelfInterview(Session.SessionStatus.RUNNING, "N");
            long endedCount = sessionRepository.countBySessionStatusAndIsSelfInterview(Session.SessionStatus.ENDED, "N");
            
            model.addAttribute("sessions", sessionList);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("currentPage", page);
            model.addAttribute("serverCurrentPage", page);
            model.addAttribute("totalPages", sessionPage.getTotalPages());
            model.addAttribute("totalItems", sessionPage.getTotalElements());
            model.addAttribute("pageSize", size);
            model.addAttribute("statusFilter", status);
            model.addAttribute("keyword", keyword);
            model.addAttribute("totalCount", totalCount);
            model.addAttribute("plannedCount", plannedCount);
            model.addAttribute("runningCount", runningCount);
            model.addAttribute("endedCount", endedCount);
            
            log.info("세션 목록 로드 완료 - {}개 세션. 현재 페이지: {}/{} 사용자: {}", 
                    sessionList.size(), page, sessionPage.getTotalPages(), currentUsername);
            
            return "session/list";
            
        } catch (Exception e) {
            log.error("세션 목록 로드 실패", e);
            model.addAttribute("error", "세션 목록을 불러올 수 없습니다: " + e.getMessage());
            return "error";
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
    @Transactional(readOnly = true)
    public String showSessionDetail(@PathVariable("id") Long sessionId, Model model, 
                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            log.info("세션 상세 조회 - sessionId: {}", sessionId);
            
            Session session = sessionRepository.findByIdWithHostAndQuestions(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다"));
            
            log.info("세션 로드 완료 - title: {}", session.getTitle());
            
            List<Question> questions = session.getQuestions();
            if (questions == null) {
                questions = new ArrayList<>();
            }
            
            List<Answer> answers = answerRepository.findBySessionIdOrderByCreatedAt(sessionId);
            
            Map<Long, List<Map<String, Object>>> answersByQuestion = new HashMap<>();
            
            for (Answer answer : answers) {
                Long questionId = answer.getQuestion().getId();
                
                Map<String, Object> data = new HashMap<>();
                data.put("answer", answer);
                
                boolean hasAiFeedback = answer.getFeedbacks().stream()
                    .anyMatch(f -> f.getFeedbackType() == Feedback.FeedbackType.AI && f.getScore() != null);
                data.put("hasAiFeedback", hasAiFeedback);
                
                if (hasAiFeedback) {
                    Feedback aiFeedback = answer.getFeedbacks().stream()
                        .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.AI && f.getScore() != null)
                        .findFirst()
                        .orElse(null);
                    data.put("aiFeedback", aiFeedback);
                }
                
                boolean hasInterviewerFeedback = answer.getFeedbacks().stream()
                    .anyMatch(f -> f.getFeedbackType() == Feedback.FeedbackType.INTERVIEWER && f.getScore() != null);
                data.put("hasInterviewerFeedback", hasInterviewerFeedback);
                
                if (hasInterviewerFeedback) {
                    Feedback interviewerFeedback = answer.getFeedbacks().stream()
                        .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.INTERVIEWER && f.getScore() != null)
                        .findFirst()
                        .orElse(null);
                    data.put("interviewerFeedback", interviewerFeedback);
                }
                
                answersByQuestion.computeIfAbsent(questionId, k -> new ArrayList<>()).add(data);
            }
            
            long totalAnswerCount = answers.size();
            long answeredQuestionCount = answersByQuestion.size();
            
            model.addAttribute("interviewSession", session);
            model.addAttribute("questions", questions);
            model.addAttribute("answersByQuestion", answersByQuestion);
            model.addAttribute("totalAnswerCount", totalAnswerCount);
            model.addAttribute("answeredQuestionCount", answeredQuestionCount);
            
            if (userDetails != null) {
                User currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElse(null);
                model.addAttribute("currentUser", currentUser);
            }
            
            log.info("세션 상세 로드 완료 - sessionId: {}, questions: {}", sessionId, questions.size());
            return "session/detail";
            
        } catch (Exception e) {
            log.error("세션 상세 조회 실패 - sessionId: {}", sessionId, e);
            model.addAttribute("error", "세션을 불러올 수 없습니다: " + e.getMessage());
            return "error/500";
        }
    }
}