package com.mockerview.controller.web;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.*;
import com.mockerview.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/session")
@RequiredArgsConstructor
public class SessionWebController {

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    @Value("${agora.app-id}")
    private String agoraAppId;

    @GetMapping("/list")
    @Transactional(readOnly = true)
    public String listSessions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        
        log.info("세션 목록 로드 중 - status: {}, keyword: {}", status, keyword);
        
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Session> sessionPage;
        
        if (status != null && !status.isEmpty()) {
            Session.SessionStatus sessionStatus = Session.SessionStatus.valueOf(status);
            sessionPage = sessionRepository.findByStatusPageable(sessionStatus, pageable);
        } else if (keyword != null && !keyword.isEmpty()) {
            sessionPage = sessionRepository.searchSessionsPageable(keyword, null, pageable);
        } else {
            sessionPage = sessionRepository.findAllSessionsWithHost(pageable);
        }
        
        for (Session session : sessionPage.getContent()) {
            if (session.getHost() != null) {
                session.getHost().getName();
            }
        }
        
        model.addAttribute("sessions", sessionPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", sessionPage.getTotalPages());
        model.addAttribute("totalItems", sessionPage.getTotalElements());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);
        
        log.info("세션 목록 로드 완료 - {} 개 세션. 현재 페이지: {}/{} 사용자: {}", 
                sessionPage.getContent().size(), page + 1, sessionPage.getTotalPages(), 
                currentUser.getUsername());
        
        return "session/list";
    }

    @GetMapping("/{id}/join")
    public String joinSession(@PathVariable Long id,
                            @RequestParam(required = false) String role,
                            @AuthenticationPrincipal CustomUserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            log.info("세션 역할 설정 - userId: {}, role: {}", user.getId(), role);
            
            return "redirect:/session/" + id + (role != null ? "?role=" + role : "");
        } catch (Exception e) {
            log.error("세션 참가 실패", e);
            redirectAttributes.addFlashAttribute("error", "세션 참가에 실패했습니다.");
            return "redirect:/session/list";
        }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public String showSession(@PathVariable Long id,
                            @RequestParam(required = false) String role,
                            @AuthenticationPrincipal CustomUserDetails userDetails,
                            Model model) {
        try {
            User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            log.info("세션 접속 - sessionId: {}, userId: {}, userName: {}, role: {}", 
                    id, currentUser.getId(), currentUser.getName(), role);
            
            Session session = sessionRepository.findByIdWithHost(id)
                .orElseThrow(() -> new RuntimeException("Session not found: " + id));
            
            List<Question> questions = questionRepository.findBySessionIdOrderByOrderNo(id);
            for (Question question : questions) {
                question.getText();
                if (question.getQuestioner() != null) {
                    question.getQuestioner().getName();
                }
            }
            
            List<Answer> answers = answerRepository.findByQuestionSessionId(id);
            for (Answer answer : answers) {
                if (answer.getUser() != null) {
                    answer.getUser().getName();
                }
                answer.getAnswerText();
            }
            
            boolean isHost = session.getHost() != null && 
                            session.getHost().getId().equals(currentUser.getId());
            
            String userRole = role != null ? role : (isHost ? "HOST" : "STUDENT");
            
            String sessionType = session.getSessionType() != null ? 
                                session.getSessionType() : "TEXT";
            
            log.info("세션 로드 완료 - 사용자: {}, 역할: {}, 호스트여부: {}, 타입: {}", 
                    currentUser.getName(), userRole, isHost, sessionType);
            
            model.addAttribute("session", session);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("isHost", isHost);
            model.addAttribute("userRole", userRole);
            model.addAttribute("questions", questions);
            model.addAttribute("answers", answers);
            model.addAttribute("sessionType", sessionType);
            model.addAttribute("sessionHost", session.getHost());
            model.addAttribute("hostName", session.getHost() != null ? session.getHost().getName() : "알 수 없음");
            
            if ("VIDEO".equals(sessionType) && session.getAgoraChannel() != null) {
                model.addAttribute("agoraChannel", session.getAgoraChannel());
                model.addAttribute("agoraAppId", agoraAppId);
            }
            
            return "session/session";
        } catch (Exception e) {
            log.error("❌ 세션 로드 실패 - sessionId: {}, error: {}", id, e.getMessage(), e);
            model.addAttribute("error", "세션을 불러오는데 실패했습니다: " + e.getMessage());
            return "redirect:/session/list";
        }
    }

    @GetMapping("/create")
    public String showCreateForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        model.addAttribute("currentUser", currentUser);
        return "session/create";
    }

    @PostMapping("/create")
    @Transactional
    public String createSession(@ModelAttribute Session session,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        try {
            User host = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            session.setHost(host);
            session.setCreatedAt(LocalDateTime.now());
            session.setSessionStatus(Session.SessionStatus.PLANNED);
            
            if (session.getExpiresAt() == null) {
                session.setExpiresAt(LocalDateTime.now().plusHours(3));
            }
            
            if (session.getSessionType() == null) {
                session.setSessionType("TEXT");
            }
            
            if ("VIDEO".equals(session.getSessionType())) {
                session.setAgoraChannel("session_" + System.currentTimeMillis());
                session.setMediaEnabledBoolean(true);
            }
            
            Session savedSession = sessionRepository.save(session);
            
            log.info("세션 생성 완료 - ID: {}, 호스트: {}, 타입: {}", 
                    savedSession.getId(), host.getName(), savedSession.getSessionType());
            
            redirectAttributes.addFlashAttribute("message", "세션이 생성되었습니다.");
            return "redirect:/session/" + savedSession.getId() + "/join?role=HOST";
        } catch (Exception e) {
            log.error("세션 생성 실패", e);
            redirectAttributes.addFlashAttribute("error", "세션 생성에 실패했습니다.");
            return "redirect:/session/create";
        }
    }

    @PostMapping("/{id}/end")
    @Transactional
    public String endSession(@PathVariable Long id,
                            @AuthenticationPrincipal CustomUserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));
            
            if (!session.getHost().getId().equals(currentUser.getId())) {
                throw new RuntimeException("권한이 없습니다.");
            }
            
            session.setSessionStatus(Session.SessionStatus.ENDED);
            session.setEndTime(LocalDateTime.now());
            sessionRepository.save(session);
            
            log.info("세션 종료 - sessionId: {}, 호스트: {}", id, currentUser.getName());
            
            redirectAttributes.addFlashAttribute("message", "세션이 종료되었습니다.");
            return "redirect:/session/list";
        } catch (Exception e) {
            log.error("세션 종료 실패", e);
            redirectAttributes.addFlashAttribute("error", "세션 종료에 실패했습니다.");
            return "redirect:/session/" + id;
        }
    }

    @GetMapping("/{id}/scoreboard")
    @Transactional(readOnly = true)
    public String showScoreboard(@PathVariable Long id, Model model) {
        try {
            Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));
            
            if (session.getHost() != null) {
                session.getHost().getName();
            }
            
            List<Question> questions = questionRepository.findBySessionIdOrderByOrderNo(id);
            for (Question question : questions) {
                question.getText();
            }
            
            List<Answer> answers = answerRepository.findByQuestionSessionId(id);
            
            Map<Long, Map<Long, Answer>> scoreBoard = new HashMap<>();
            for (Answer answer : answers) {
                if (answer.getUser() != null) {
                    answer.getUser().getName();
                }
                answer.getAnswerText();
                
                Long userId = answer.getUser().getId();
                Long questionId = answer.getQuestion().getId();
                
                scoreBoard.computeIfAbsent(userId, k -> new HashMap<>())
                        .put(questionId, answer);
            }
            
            model.addAttribute("session", session);
            model.addAttribute("questions", questions);
            model.addAttribute("answers", answers);
            model.addAttribute("scoreBoard", scoreBoard);
            
            return "session/scoreboard";
        } catch (Exception e) {
            log.error("점수판 로드 실패", e);
            return "redirect:/session/list";
        }
    }
}