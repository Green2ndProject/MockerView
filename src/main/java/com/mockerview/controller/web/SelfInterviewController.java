package com.mockerview.controller.web;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.Session;
import com.mockerview.entity.Question;
import com.mockerview.entity.Answer;
import com.mockerview.entity.User;
import com.mockerview.repository.SessionRepository;
import com.mockerview.repository.QuestionRepository;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/selfinterview")
@RequiredArgsConstructor
@Slf4j
public class SelfInterviewController {

    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final com.mockerview.repository.FeedbackRepository feedbackRepository;

    @GetMapping("/create")
    public String createPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userRepository.findById(userDetails.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        model.addAttribute("user", user);
        return "selfinterview/create";
    }

    @GetMapping("/create-ai")
    public String createAIPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userRepository.findById(userDetails.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        model.addAttribute("user", user);
        return "selfinterview/create-ai";
    }

    @GetMapping("/room/{sessionId}")
    public String room(@PathVariable Long sessionId,
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        Model model) {
        
        User user = userRepository.findById(userDetails.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));
        
        List<Question> questions = questionRepository.findBySessionIdOrderByOrderNoAsc(sessionId);
        
        log.info("셀프면접 룸 로드 - sessionId: {}, userId: {}, 질문수: {}", 
                sessionId, user.getId(), questions.size());
        
        model.addAttribute("user", user);
        model.addAttribute("session", session);
        model.addAttribute("questions", questions);
        
        return "selfinterview/room";
    }

    @GetMapping("/list")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String list(
            @AuthenticationPrincipal CustomUserDetails userDetails, 
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "6") int size,
            Model model) {
        try {
            log.info("셀프 면접 리스트 조회 시작 - userId: {}, page: {}", userDetails.getUserId(), page);
            
            User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Session> sessionPage = sessionRepository.findSelfInterviewsByHostIdPageable(user.getId(), pageable);
            
            List<Session> sessions = sessionPage.getContent();
            
            for (Session session : sessions) {
                if (session.getQuestions() != null) {
                    session.getQuestions().size();
                    
                    log.info("세션 ID: {}, 질문 수: {}", session.getId(), session.getQuestions().size());
                    
                    List<Answer> allAnswers = session.getQuestions().stream()
                        .flatMap(q -> answerRepository.findByQuestionIdOrderByCreatedAtAsc(q.getId()).stream())
                        .toList();
                    
                    log.info("세션 ID: {}, 전체 답변 수: {}", session.getId(), allAnswers.size());
                    
                    if (!allAnswers.isEmpty()) {
                        List<com.mockerview.entity.Feedback> aiFeedbacks = 
                            feedbackRepository.findByAnswerInAndFeedbackType(
                                allAnswers, 
                                com.mockerview.entity.Feedback.FeedbackType.AI
                            );
                        
                        log.info("세션 ID: {}, AI 피드백 수: {}", session.getId(), aiFeedbacks.size());
                        
                        if (!aiFeedbacks.isEmpty()) {
                            double avgScore = aiFeedbacks.stream()
                                .filter(f -> f.getScore() != null)
                                .mapToInt(com.mockerview.entity.Feedback::getScore)
                                .average()
                                .orElse(0.0);
                            
                            log.info("세션 ID: {}, AI 평균 점수: {}", session.getId(), avgScore);
                            
                            session.setAvgScore(avgScore);
                        } else {
                            log.warn("세션 ID: {}, AI 피드백이 없습니다!", session.getId());
                            session.setAvgScore(null);
                        }
                    } else {
                        log.warn("세션 ID: {}, 답변이 없습니다!", session.getId());
                    }
                }
            }
            
            int totalPages = sessionPage.getTotalPages();
            int startPage = Math.max(1, page - 2);
            int endPage = Math.min(totalPages, page + 2);
            
            log.info("✅ 셀프 면접 리스트 조회 완료 - userId: {}, 세션 수: {}, 현재 페이지: {}/{}", 
                    user.getId(), sessions.size(), page, totalPages);
            
            model.addAttribute("user", user);
            model.addAttribute("sessions", sessions);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("startPage", startPage);
            model.addAttribute("endPage", endPage);
            model.addAttribute("totalItems", sessionPage.getTotalElements());
            
            return "selfinterview/list";
            
        } catch (Exception e) {
            log.error("❌ 셀프 면접 리스트 조회 실패 - userId: {}", userDetails.getUserId(), e);
            model.addAttribute("user", new User());
            model.addAttribute("sessions", List.of());
            model.addAttribute("error", "리스트를 불러올 수 없습니다: " + e.getMessage());
            return "selfinterview/list";
        }
    }

    @GetMapping("/history")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String history(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        try {
            log.info("셀프 면접 기록 조회 시작 - userId: {}", userDetails.getUserId());
            
            User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            List<Session> sessions = sessionRepository.findSelfInterviewsByHostId(user.getId());
            
            for (Session session : sessions) {
                if (session.getQuestions() != null) {
                    session.getQuestions().size();
                    
                    List<Answer> allAnswers = session.getQuestions().stream()
                        .flatMap(q -> answerRepository.findByQuestionIdOrderByCreatedAtAsc(q.getId()).stream())
                        .toList();
                    
                    if (!allAnswers.isEmpty()) {
                        List<com.mockerview.entity.Feedback> aiFeedbacks = 
                            feedbackRepository.findByAnswerInAndFeedbackType(
                                allAnswers, 
                                com.mockerview.entity.Feedback.FeedbackType.AI
                            );
                        
                        if (!aiFeedbacks.isEmpty()) {
                            double avgScore = aiFeedbacks.stream()
                                .filter(f -> f.getScore() != null)
                                .mapToInt(com.mockerview.entity.Feedback::getScore)
                                .average()
                                .orElse(0.0);
                            
                            session.setAvgScore(avgScore);
                        } else {
                            session.setAvgScore(null);
                        }
                    }
                }
            }
            
            log.info("✅ 셀프 면접 기록 조회 완료 - userId: {}, 세션 수: {}", user.getId(), sessions.size());
            
            model.addAttribute("user", user);
            model.addAttribute("sessions", sessions);
            
            return "selfinterview/history";
            
        } catch (Exception e) {
            log.error("❌ 셀프 면접 기록 조회 실패 - userId: {}", userDetails.getUserId(), e);
            model.addAttribute("user", new User());
            model.addAttribute("sessions", List.of());
            model.addAttribute("error", "기록을 불러올 수 없습니다: " + e.getMessage());
            return "selfinterview/history";
        }
    }

    @GetMapping("/videos")
    public String videos(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        try {
            User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow(() -> new RuntimeException("User not found"));
            
            List<Answer> videoAnswers = answerRepository.findByUserIdAndVideoUrlIsNotNull(user.getId());
            
            model.addAttribute("user", user);
            model.addAttribute("videoAnswers", videoAnswers);
            model.addAttribute("totalVideos", videoAnswers.size());
            
            return "selfinterview/videos";
        } catch (Exception e) {
            log.error("녹화 영상 목록 로딩 실패: {}", e.getMessage(), e);
            return "redirect:/auth/mypage";
        }
    }
}
