package com.mockerview.controller.web;

import com.mockerview.entity.User;
import com.mockerview.entity.Answer;
import com.mockerview.entity.Question;
import com.mockerview.entity.Review;
import com.mockerview.entity.Session;
import com.mockerview.service.ReviewService;
import com.mockerview.service.SessionService;
import com.mockerview.service.SubscriptionService;
import com.mockerview.repository.UserRepository;
import com.mockerview.repository.SessionRepository;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {
    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);
    
    private final ReviewService reviewService;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final SessionService sessionService;
    private final SubscriptionService subscriptionService;

    @GetMapping("/list")
    @Transactional(readOnly = true)
    public String listPage(@RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "6") int size,
                            @RequestParam(defaultValue = "desc") String sortOrder, 
                            Model model) {
    
        try {
            log.info("리뷰 가능 세션 목록 로드 중 - page: {}, size: {}", page, size);
        
            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "endTime"));
            Page<Session> sessionPage = sessionService.getReviewableSessionsPageable(pageable); 
            
            for (Session session : sessionPage.getContent()) {
                session.getQuestions().size();
                if (session.getHost() != null) {
                    session.getHost().getName();
                }
            }
            
            int totalPages = sessionPage.getTotalPages();
            int startPage = Math.max(1, page - 2);
            int endPage = Math.min(totalPages, page + 2);
        
            model.addAttribute("sessions", sessionPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("serverCurrentPage", sessionPage.getNumber() + 1);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("startPage", startPage);
            model.addAttribute("endPage", endPage);
            model.addAttribute("totalItems", sessionPage.getTotalElements());

            log.info("리뷰 가능 세션 목록 로드 완료 - 세션 수: {}, 현재 페이지: {}/{}", 
                    sessionPage.getContent().size(), page, totalPages);
            
            return "review/list";
        
        } catch (Exception e) {
            log.error("리뷰 가능 세션 목록 로드 실패", e);
            model.addAttribute("error", "세션 목록을 불러올 수 없습니다: " + e.getMessage());
            
            return "error/500";
        }
    }

    @GetMapping("/my")
    @Transactional(readOnly = true)
    public String myReviews(@RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "6") int size,
                            Authentication authentication,
                            Model model) {
        try {
            User currentUser = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            
            log.info("내 리뷰 목록 로드 중 - userId: {}, page: {}", currentUser.getId(), page);
            
            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Review> reviewPage = reviewService.getMyReviewsPageable(currentUser.getId(), pageable);
            
            // Lazy loading 강제 초기화
            for (Review review : reviewPage.getContent()) {
                if (review.getSession() != null) {
                    review.getSession().getTitle();
                    if (review.getSession().getHost() != null) {
                        review.getSession().getHost().getName();
                    }
                }
                if (review.getReviewer() != null) {
                    review.getReviewer().getName();
                }
                if (review.getAnswer() != null) {
                    review.getAnswer().getAnswerText();
                    if (review.getAnswer().getQuestion() != null) {
                        review.getAnswer().getQuestion().getText();
                    }
                }
            }
            
            int totalPages = Math.max(1, reviewPage.getTotalPages());
            int startPage = Math.max(1, page - 2);
            int endPage = Math.min(totalPages, page + 2);
            
            model.addAttribute("reviews", reviewPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("startPage", startPage);
            model.addAttribute("endPage", endPage);
            model.addAttribute("totalItems", reviewPage.getTotalElements());
            
            log.info("내 리뷰 목록 로드 완료 - 리뷰 수: {}", reviewPage.getContent().size());
            
            return "review/my";
            
        } catch (Exception e) {
            log.error("내 리뷰 목록 로드 실패", e);
            model.addAttribute("error", "리뷰 목록을 불러올 수 없습니다: " + e.getMessage());
            model.addAttribute("reviews", List.of());
            model.addAttribute("currentPage", 1);
            model.addAttribute("totalPages", 1);
            model.addAttribute("startPage", 1);
            model.addAttribute("endPage", 1);
            model.addAttribute("totalItems", 0L);
            return "review/my";
        }
    }

    @GetMapping("/detail/{sessionId}")
    @Transactional(readOnly = true)
    public String reviewDetail(@PathVariable Long sessionId,
                                Authentication authentication,
                                Model model) {
        try {
            User currentUser = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            
            log.info("리뷰 상세 페이지 로드 중 - sessionId: {}, userId: {}", sessionId, currentUser.getId());
            
            Session session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다"));
            
            if (!session.getSessionStatus().equals(Session.SessionStatus.ENDED)) {
                throw new RuntimeException("아직 종료되지 않은 세션입니다");
            }
            
            List<Question> questions = questionRepository.findBySessionIdOrderByOrderNo(sessionId);
            List<Answer> answers = answerRepository.findByQuestionSessionIdOrderByCreatedAt(sessionId);
            
            Map<Long, List<Answer>> answersByQuestion = answers.stream()
                .collect(Collectors.groupingBy(answer -> answer.getQuestion().getId()));
            
            boolean hasAlreadyReviewed = false;
            if (!answers.isEmpty()) {
                Long firstAnswerId = answers.get(0).getId();
                hasAlreadyReviewed = reviewService.existsByReviewerIdAndAnswerId(currentUser.getId(), firstAnswerId);
            }
            
            for (Answer answer : answers) {
                if (answer.getUser() != null) {
                    answer.getUser().getName();
                }
                if (answer.getQuestion() != null) {
                    answer.getQuestion().getText();
                }
            }
            
            if (session.getHost() != null) {
                session.getHost().getName();
            }
            
            Map<Long, Boolean> canReadMap = answers.stream()
                .collect(Collectors.toMap(
                    Answer::getId,
                    answer -> {
                        try {
                            return subscriptionService.canReadReview(currentUser.getId());
                        } catch (Exception e) {
                            log.error("리뷰 읽기 권한 확인 실패 - answerId: {}", answer.getId(), e);
                            return false;
                        }
                    }
                ));
            
            model.addAttribute("interviewSession", session);
            model.addAttribute("questions", questions);
            model.addAttribute("answers", answers);
            model.addAttribute("answersByQuestion", answersByQuestion);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("hasAlreadyReviewed", hasAlreadyReviewed);
            model.addAttribute("canReadMap", canReadMap);
            
            log.info("리뷰 상세 페이지 로드 완료 - 질문 수: {}, 답변 수: {}, 이미 리뷰: {}", 
                questions.size(), answers.size(), hasAlreadyReviewed);
            
            return "review/detail";
            
        } catch (Exception e) {
            log.error("리뷰 상세 페이지 로드 실패 - sessionId: {}", sessionId, e);
            model.addAttribute("error", "리뷰 페이지를 불러올 수 없습니다: " + e.getMessage());
            return "error/500";
        }
    }
}