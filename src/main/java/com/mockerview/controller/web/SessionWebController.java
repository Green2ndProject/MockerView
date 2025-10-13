package com.mockerview.controller.web;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.Answer;
import com.mockerview.entity.Feedback;
import com.mockerview.entity.Question;
import com.mockerview.entity.Session;
import com.mockerview.entity.User;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.FeedbackRepository;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final FeedbackRepository feedbackRepository;

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

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public String showSession(@PathVariable Long id,
                            @RequestParam(required = false) String role,
                            @AuthenticationPrincipal CustomUserDetails userDetails,
                            Model model) {
        
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Session session = sessionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Session not found: " + id));
        
        boolean isHost = session.getHost().getId().equals(currentUser.getId());
        
        log.info("세션 접속 - sessionId: {}, userId: {}, userName: {}, role: {}", 
            id, currentUser.getId(), currentUser.getName(), role);
        
        model.addAttribute("session", session);
        model.addAttribute("sessionId", session.getId());
        model.addAttribute("sessionTitle", session.getTitle());
        model.addAttribute("sessionType", session.getSessionType());
        model.addAttribute("userId", currentUser.getId());
        model.addAttribute("userName", currentUser.getName());
        model.addAttribute("isHost", isHost);
        model.addAttribute("sessionHost", session.getHost());
        
        log.info("세션 로드 완료 - 사용자: {}, 역할: {}, 호스트여부: {}, 타입: {}", 
            currentUser.getName(), role, isHost, session.getSessionType());
        
        return "session/session";
    }

    @GetMapping("/detail/{id}")
    @Transactional(readOnly = true)
    public String sessionDetail(@PathVariable Long id, Model model) {
        Session session = sessionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Session not found"));
        
        List<Answer> answers = answerRepository.findAllBySessionIdWithFeedbacks(id);
        
        Map<Long, List<Map<String, Object>>> answersByQuestion = new HashMap<>();
        for (Answer answer : answers) {
            if (answer.getQuestion() != null) {
                Long questionId = answer.getQuestion().getId();
                answersByQuestion.putIfAbsent(questionId, new ArrayList<>());
                
                Map<String, Object> answerItem = new HashMap<>();
                answerItem.put("answer", answer);
                
                Feedback aiFeedback = answer.getFeedbacks().stream()
                    .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.AI)
                    .findFirst().orElse(null);
                Feedback interviewerFeedback = answer.getFeedbacks().stream()
                    .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.INTERVIEWER)
                    .findFirst().orElse(null);
                
                answerItem.put("aiFeedback", aiFeedback);
                answerItem.put("interviewerFeedback", interviewerFeedback);
                answerItem.put("hasAiFeedback", aiFeedback != null);
                answerItem.put("hasInterviewerFeedback", interviewerFeedback != null);
                
                answersByQuestion.get(questionId).add(answerItem);
            }
        }
        
        List<Question> questions = questionRepository.findBySessionIdOrderByOrderNoAsc(session.getId());
        long totalAnswerCount = answers.size();
        long answeredQuestionCount = answersByQuestion.size();
        
        model.addAttribute("session", session);
        model.addAttribute("questions", questions);
        model.addAttribute("answersByQuestion", answersByQuestion);
        model.addAttribute("totalAnswerCount", totalAnswerCount);
        model.addAttribute("answeredQuestionCount", answeredQuestionCount);
        
        return "session/detail";
    }

    @GetMapping("/scoreboard/{id}")
    @Transactional(readOnly = true)
    public String scoreboard(@PathVariable Long id, Model model) {
        Session session = sessionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Session not found"));
        
        List<Answer> allAnswers = answerRepository.findAllBySessionIdWithFeedbacks(id);
        
        Map<User, List<Answer>> answersByUser = allAnswers.stream()
            .collect(Collectors.groupingBy(Answer::getUser));
        
        List<Map<String, Object>> userScores = new ArrayList<>();
        
        for (Map.Entry<User, List<Answer>> entry : answersByUser.entrySet()) {
            User user = entry.getKey();
            List<Answer> userAnswers = entry.getValue();
            
            double avgAiScore = userAnswers.stream()
                .flatMap(a -> a.getFeedbacks().stream())
                .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.AI && f.getScore() != null)
                .mapToInt(Feedback::getScore)
                .average()
                .orElse(0.0);
            
            double avgInterviewerScore = userAnswers.stream()
                .flatMap(a -> a.getFeedbacks().stream())
                .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.INTERVIEWER && f.getScore() != null)
                .mapToInt(Feedback::getScore)
                .average()
                .orElse(0.0);
            
            double totalScore = (avgAiScore + avgInterviewerScore) / 2.0;
            
            Map<String, Object> scoreData = new HashMap<>();
            scoreData.put("user", user);
            scoreData.put("answers", userAnswers);
            scoreData.put("answerCount", userAnswers.size());
            scoreData.put("avgAiScore", Math.round(avgAiScore));
            scoreData.put("avgInterviewerScore", Math.round(avgInterviewerScore));
            scoreData.put("totalScore", Math.round(totalScore));
            
            userScores.add(scoreData);
        }
        
        userScores.sort((a, b) -> 
            ((Integer) b.get("totalScore")).compareTo((Integer) a.get("totalScore"))
        );
        
        long totalQuestions = questionRepository.countBySessionId(id);
        
        model.addAttribute("interviewSession", session);
        model.addAttribute("userScores", userScores);
        model.addAttribute("totalQuestions", totalQuestions);
        
        return "session/scoreboard";
    }

    @GetMapping("/scoreboard/{id}/download/csv")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadScoreboardCsv(@PathVariable Long id) {
        Session session = sessionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Session not found"));
        
        List<Answer> allAnswers = answerRepository.findByQuestionSessionId(id);
        
        Map<Long, User> userMap = new HashMap<>();
        Map<Long, List<Long>> userAnswerIds = new HashMap<>();
        
        for (Answer answer : allAnswers) {
            Long userId = answer.getUser().getId();
            userMap.put(userId, answer.getUser());
            userAnswerIds.computeIfAbsent(userId, k -> new ArrayList<>()).add(answer.getId());
        }
        
        StringBuilder csv = new StringBuilder();
        csv.append("순위,이름,답변수,AI평균,면접관평균,총점\n");
        
        List<Map<String, Object>> userScores = new ArrayList<>();
        
        for (Map.Entry<Long, List<Long>> entry : userAnswerIds.entrySet()) {
            Long userId = entry.getKey();
            List<Long> answerIds = entry.getValue();
            User user = userMap.get(userId);
            
            double avgAiScore = 0;
            double avgInterviewerScore = 0;
            int aiCount = 0;
            int interviewerCount = 0;
            
            for (Long answerId : answerIds) {
                List<Feedback> feedbacks = feedbackRepository.findByAnswerId(answerId);
                for (Feedback f : feedbacks) {
                    if (f.getFeedbackType() == Feedback.FeedbackType.AI && f.getScore() != null) {
                        avgAiScore += f.getScore();
                        aiCount++;
                    } else if (f.getFeedbackType() == Feedback.FeedbackType.INTERVIEWER && f.getScore() != null) {
                        avgInterviewerScore += f.getScore();
                        interviewerCount++;
                    }
                }
            }
            
            avgAiScore = aiCount > 0 ? avgAiScore / aiCount : 0;
            avgInterviewerScore = interviewerCount > 0 ? avgInterviewerScore / interviewerCount : 0;
            double totalScore = (avgAiScore + avgInterviewerScore) / 2.0;
            
            Map<String, Object> scoreData = new HashMap<>();
            scoreData.put("user", user);
            scoreData.put("answerCount", answerIds.size());
            scoreData.put("avgAiScore", Math.round(avgAiScore));
            scoreData.put("avgInterviewerScore", Math.round(avgInterviewerScore));
            scoreData.put("totalScore", Math.round(totalScore));
            
            userScores.add(scoreData);
        }
        
        userScores.sort((a, b) -> 
            ((Integer) b.get("totalScore")).compareTo((Integer) a.get("totalScore"))
        );
        
        int rank = 1;
        for (Map<String, Object> score : userScores) {
            User user = (User) score.get("user");
            csv.append(rank++).append(",")
            .append(user.getName()).append(",")
            .append(score.get("answerCount")).append(",")
            .append(score.get("avgAiScore")).append(",")
            .append(score.get("avgInterviewerScore")).append(",")
            .append(score.get("totalScore")).append("\n");
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", 
            session.getTitle() + "_scoreboard.csv");
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(csv.toString().getBytes(StandardCharsets.UTF_8));
        }
    }