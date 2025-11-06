package com.mockerview.controller.web;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.Answer;
import com.mockerview.entity.Feedback;
import com.mockerview.entity.Question;
import com.mockerview.entity.Session;
import com.mockerview.entity.SessionParticipant;
import com.mockerview.entity.Subscription;
import com.mockerview.entity.User;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.QuestionRepository;
import com.mockerview.repository.SessionRepository;
import com.mockerview.repository.SessionParticipantRepository;
import com.mockerview.repository.UserRepository;
import com.mockerview.service.AgoraService;
import com.mockerview.service.SessionService;
import com.mockerview.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
    private final SubscriptionService subscriptionService;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final AgoraService agoraService;
    
    @Value("${agora.app-id:}")
    private String agoraAppId;

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
    public String sessionDetail(@PathVariable Long sessionId,
                                    @RequestParam(required = false) String role,
                                    Authentication authentication,
                                    Model model) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                log.warn("비로그인 사용자 세션 접근 시도");
                return "redirect:/auth/login";
            }
            
            Session session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다"));
            
            if (session.getHost() != null) {
                session.getHost().getName();
            }
            
            List<Question> questions = questionRepository.findBySessionIdOrderByOrderNo(sessionId);
            for (Question question : questions) {
                question.getAnswers().size();
            }
            
            User.UserRole selectedRole = User.UserRole.STUDENT;
            if (role != null) {
                try {
                    selectedRole = User.UserRole.valueOf(role);
                } catch (IllegalArgumentException e) {
                    selectedRole = User.UserRole.STUDENT;
                }
            }
            
            boolean isHost = session.getHost() != null && 
                            session.getHost().getId().equals(currentUser.getId());
            
            Subscription subscription = subscriptionService.getActiveSubscription(currentUser.getId());
            boolean isPremium = subscription != null && 
                    (subscription.getPlanType() == Subscription.PlanType.PRO ||
                        subscription.getPlanType() == Subscription.PlanType.TEAM ||
                        subscription.getPlanType() == Subscription.PlanType.ENTERPRISE);
            
            model.addAttribute("session", session);
            model.addAttribute("sessionId", sessionId);
            model.addAttribute("sessionTitle", session.getTitle());
            model.addAttribute("sessionType", session.getSessionType());
            model.addAttribute("mediaEnabled", session.getMediaEnabled());
            model.addAttribute("questions", questions);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("selectedRole", selectedRole);
            model.addAttribute("isHost", isHost);
            model.addAttribute("isPremium", isPremium);
            model.addAttribute("sessionStatus", session.getSessionStatus());
            model.addAttribute("aiEnabled", session.getAiEnabled());
            model.addAttribute("agoraChannel", session.getAgoraChannel());
            model.addAttribute("isSelfInterview", "Y".equals(session.getIsSelfInterview()));
            
            log.info("세션 상세 페이지 로드 - sessionId: {}, userId: {}, role: {}, type: {}, isHost: {}, isSelfInterview: {}", 
                    sessionId, currentUser.getId(), selectedRole, session.getSessionType(), isHost, session.getIsSelfInterview());
            
            return "session/session";
            
        } catch (Exception e) {
            log.error("세션 상세 페이지 로드 실패 - sessionId: {}", sessionId, e);
            model.addAttribute("error", "세션을 불러올 수 없습니다: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/detail/{sessionId}")
    public String sessionResultDetail(@PathVariable Long sessionId, 
                                        Authentication authentication,
                                        Model model) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                log.warn("비로그인 사용자 세션 결과 접근 시도");
                return "redirect:/auth/login";
            }
            
            Session session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다"));
            
            List<Question> questions = questionRepository.findBySessionIdOrderByOrderNo(sessionId);
            List<Answer> answers = answerRepository.findByQuestionSessionIdOrderByCreatedAt(sessionId);
            
            Map<Long, List<AnswerWithFeedback>> answersByQuestion = new HashMap<>();
            
            for (Answer answer : answers) {
                Long questionId = answer.getQuestion().getId();
                answersByQuestion.putIfAbsent(questionId, new ArrayList<>());
                
                AnswerWithFeedback awf = new AnswerWithFeedback();
                awf.setAnswer(answer);
                
                List<Feedback> feedbacks = answer.getFeedbacks();
                Feedback aiFeedback = null;
                Feedback interviewerFeedback = null;
                
                if (feedbacks != null && !feedbacks.isEmpty()) {
                    for (Feedback fb : feedbacks) {
                        if (fb.getReviewer() == null) {
                            aiFeedback = fb;
                        } else {
                            interviewerFeedback = fb;
                        }
                    }
                }
                
                awf.setAiFeedback(aiFeedback);
                awf.setInterviewerFeedback(interviewerFeedback);
                awf.setHasAiFeedback(aiFeedback != null);
                awf.setHasInterviewerFeedback(interviewerFeedback != null);
                
                answersByQuestion.get(questionId).add(awf);
            }
            
            model.addAttribute("session", session);
            model.addAttribute("sessionId", sessionId);
            model.addAttribute("questions", questions);
            model.addAttribute("answersByQuestion", answersByQuestion);
            model.addAttribute("currentUser", currentUser);
            
            log.info("✅ 세션 결과 상세 페이지 로드 완료 - sessionId: {}, userId: {}, 질문수: {}", 
                    sessionId, currentUser.getId(), questions.size());
            
            return "session/detail";
            
        } catch (Exception e) {
            log.error("❌ 세션 결과 상세 페이지 로드 실패 - sessionId: {}", sessionId, e);
            model.addAttribute("error", "세션 결과를 불러올 수 없습니다: " + e.getMessage());
            model.addAttribute("questions", new ArrayList<>());
            model.addAttribute("answersByQuestion", new HashMap<>());
            return "session/detail";
        }
    }

    @PostMapping("/create")
    public String createSession(@RequestParam String title,
                                    @RequestParam String sessionType,
                                    @RequestParam(required = false) String mediaEnabled,
                                    Authentication authentication,
                                    Model model) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                log.warn("비로그인 사용자 세션 생성 시도");
                return "redirect:/auth/login";
            }
            
            short mediaEnabledValue;
            if ("VIDEO".equalsIgnoreCase(sessionType)) {
                mediaEnabledValue = (short) 2;
            } else if ("AUDIO".equalsIgnoreCase(sessionType)) {
                mediaEnabledValue = (short) 1;
            } else {
                mediaEnabledValue = (short) 0;
            }
            
            Session savedSession = subscriptionService.createTeamSessionAtomic(
                currentUser.getId(), title, sessionType, mediaEnabledValue);
            
            log.info("세션 생성 완료 - sessionId: {}, userId: {}, type: {}, media: {}", 
                    savedSession.getId(), currentUser.getId(), sessionType, mediaEnabledValue);
            
            return "redirect:/session/list";
            
        } catch (RuntimeException e) {
            if ("SESSION_LIMIT_EXCEEDED".equals(e.getMessage())) {
                log.warn("세션 한도 초과로 생성 실패 - userId: {}", getCurrentUser().getId());
                return "redirect:/session/list?error=SESSION_LIMIT_EXCEEDED";
            }
            log.error("세션 생성 실패", e);
            model.addAttribute("error", "세션 생성 실패: " + e.getMessage());
            return "redirect:/session/list?error=create_failed";
        } catch (Exception e) {
            log.error("세션 생성 실패", e);
            model.addAttribute("error", "세션 생성 실패: " + e.getMessage());
            return "redirect:/session/list?error=create_failed";
        }
    }

    @GetMapping("/list")
    @Transactional(readOnly = true)
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
                .filter(session -> session != null)
                .map(session -> {
                    if (session.getHost() != null) {
                        session.getHost().getName();
                    }
                    
                    Map<String, Object> sessionMap = new HashMap<>();
                    sessionMap.put("id", session.getId());
                    sessionMap.put("title", session.getTitle());
                    sessionMap.put("description", session.getDescription());
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
            
            int totalPages = sessionPage.getTotalPages();
            int startPage = Math.max(1, page - 2);
            int endPage = Math.min(totalPages, page + 2);
            
            model.addAttribute("sessions", sessionList);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("currentPage", page);
            model.addAttribute("serverCurrentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("startPage", startPage);
            model.addAttribute("endPage", endPage);
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
    
    public static class AnswerWithFeedback {
        private Answer answer;
        private Feedback aiFeedback;
        private Feedback interviewerFeedback;
        private boolean hasAiFeedback;
        private boolean hasInterviewerFeedback;

        public Answer getAnswer() {
            return answer;
        }

        public void setAnswer(Answer answer) {
            this.answer = answer;
        }

        public Feedback getAiFeedback() {
            return aiFeedback;
        }

        public void setAiFeedback(Feedback aiFeedback) {
            this.aiFeedback = aiFeedback;
        }

        public Feedback getInterviewerFeedback() {
            return interviewerFeedback;
        }

        public void setInterviewerFeedback(Feedback interviewerFeedback) {
            this.interviewerFeedback = interviewerFeedback;
        }

        public boolean isHasAiFeedback() {
            return hasAiFeedback;
        }

        public void setHasAiFeedback(boolean hasAiFeedback) {
            this.hasAiFeedback = hasAiFeedback;
        }

        public boolean isHasInterviewerFeedback() {
            return hasInterviewerFeedback;
        }

        public void setHasInterviewerFeedback(boolean hasInterviewerFeedback) {
            this.hasInterviewerFeedback = hasInterviewerFeedback;
        }
    }


    @GetMapping("/interview/{sessionId}")
    public String interviewPage(@PathVariable Long sessionId, Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다"));

        SessionParticipant participant = sessionParticipantRepository
                .findBySessionIdAndUserId(sessionId, currentUser.getId())
                .orElseGet(() -> {
                    User.UserRole defaultRole = session.getHost() != null && session.getHost().getId().equals(currentUser.getId()) 
                            ? User.UserRole.HOST 
                            : User.UserRole.STUDENT;
                    
                    SessionParticipant newParticipant = SessionParticipant.builder()
                            .session(session)
                            .user(currentUser)
                            .role(defaultRole)
                            .isOnline(true)
                            .build();
                    
                    SessionParticipant saved = sessionParticipantRepository.save(newParticipant);
                    log.info("🆕 새 참가자 자동 등록: userId={}, role={}", currentUser.getId(), defaultRole);
                    return saved;
                });

        if (!participant.getIsOnline()) {
            participant.setIsOnline(true);
            sessionParticipantRepository.save(participant);
            log.info("✅ 참가자 온라인 상태 업데이트: userId={}", currentUser.getId());
        }

        User.UserRole participantRole = participant.getRole();

        String channelName = session.getAgoraChannel();
        if (channelName == null || channelName.isEmpty()) {
            channelName = "session_" + sessionId;
            session.setAgoraChannel(channelName);
            sessionRepository.save(session);
            log.warn("세션 {}에 agoraChannel이 없어서 자동 생성함: {}", sessionId, channelName);
        }

        com.mockerview.dto.AgoraTokenDTO tokenDTO = null;
        try {
            tokenDTO = agoraService.generateToken(channelName, currentUser.getId().intValue());
            log.info("Agora 토큰 생성 성공 - sessionId: {}, userId: {}, channel: {}", sessionId, currentUser.getId(), channelName);
        } catch (Exception e) {
            log.error("Agora 토큰 생성 실패", e);
            tokenDTO = com.mockerview.dto.AgoraTokenDTO.builder()
                    .token(null)
                    .channel(channelName)
                    .appId(agoraAppId)
                    .uid(currentUser.getId().intValue())
                    .expireTime(0L)
                    .build();
        }

        log.info("=== Interview Page 렌더링 ===");
        log.info("Session ID: {}", sessionId);
        log.info("User ID: {}", currentUser.getId());
        log.info("User Role: {}", participantRole);
        log.info("Agora App ID: {}", tokenDTO.getAppId());
        log.info("Agora Channel: {}", tokenDTO.getChannel());
        log.info("Agora Token: {}", tokenDTO.getToken() != null ? tokenDTO.getToken().substring(0, Math.min(30, tokenDTO.getToken().length())) + "..." : "null (Testing Mode)");

        model.addAttribute("sessionId", sessionId);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("participantRole", participantRole);
        model.addAttribute("agoraToken", tokenDTO);

        return "session/interview";
    }

    @GetMapping("/report/{sessionId}")
    public String reportPage(@PathVariable Long sessionId, Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다"));

        List<Question> questions = questionRepository.findBySessionId(sessionId);
        
        long interviewerCount = userRepository.countByRole(User.UserRole.HOST);
        long intervieweeCount = userRepository.countByRole(User.UserRole.STUDENT);
        
        model.addAttribute("session", session);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("questionCount", questions.size());
        model.addAttribute("interviewerCount", interviewerCount);
        model.addAttribute("intervieweeCount", intervieweeCount);
        model.addAttribute("feedbacks", List.of());
        model.addAttribute("averageRating", 0.0);
        
        if (session.getStartTime() != null && session.getEndTime() != null) {
            long duration = java.time.Duration.between(
                session.getStartTime(), 
                session.getEndTime()
            ).toMinutes();
            model.addAttribute("duration", duration + "분");
        } else {
            model.addAttribute("duration", "N/A");
        }

        return "session/report";
    }
}