package com.mockerview.controller.web;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.*;
import com.mockerview.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;

    @GetMapping("/list")
    @Transactional(readOnly = true)
    public String listReviews(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            log.info("===== 리뷰 목록 로드 시작 =====");
            
            List<Session> sessions = sessionRepository.findAll();
            
            for (Session session : sessions) {
                if (session.getHost() != null) {
                    session.getHost().getName();
                }
                if (session.getQuestions() != null) {
                    session.getQuestions().size();
                }
            }
            
            log.info("✅ 리뷰 목록 로드 완료 - {} 개 세션", sessions.size());
            
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("sessions", sessions);
            
            return "review/list";
        } catch (Exception e) {
            log.error("❌ 리뷰 목록 로드 실패", e);
            model.addAttribute("error", "리뷰 목록을 불러오는데 실패했습니다.");
            return "redirect:/session/list";
        }
    }

    @GetMapping("/my")
    @Transactional(readOnly = true)
    public String myReviews(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            log.info("===== 내 리뷰 로드 시작 =====");
            
            List<Answer> myAnswers = answerRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
            
            for (Answer answer : myAnswers) {
                if (answer.getUser() != null) {
                    answer.getUser().getName();
                }
                if (answer.getQuestion() != null) {
                    answer.getQuestion().getText();
                    if (answer.getQuestion().getSession() != null) {
                        answer.getQuestion().getSession().getTitle();
                    }
                }
                answer.getAnswerText();
            }
            
            log.info("✅ 내 리뷰 로드 완료 - {} 개 답변", myAnswers.size());
            
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("answers", myAnswers);
            
            return "review/my";
        } catch (Exception e) {
            log.error("❌ 내 리뷰 로드 실패", e);
            model.addAttribute("error", "내 리뷰를 불러오는데 실패했습니다.");
            return "redirect:/review/list";
        }
    }

    @GetMapping("/detail/{id}")
    @Transactional(readOnly = true)
    public String reviewDetail(@PathVariable Long id, Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            log.info("===== 리뷰 상세 로드 시작 - Session ID: {} =====", id);
            
            Session session = sessionRepository.findByIdWithHostAndQuestions(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));
            
            if (session.getHost() != null) {
                session.getHost().getName();
            }
            
            List<Question> questions = questionRepository.findBySessionIdOrderByOrderNo(id);
            for (Question question : questions) {
                question.getText();
            }
            
            List<Answer> answers = answerRepository.findAllBySessionIdWithFeedbacks(id);
            for (Answer answer : answers) {
                if (answer.getUser() != null) {
                    answer.getUser().getName();
                }
                answer.getAnswerText();
                if (answer.getFeedbacks() != null) {
                    answer.getFeedbacks().size();
                }
            }
            
            Map<Long, List<AnswerWithFeedback>> answersByQuestion = new HashMap<>();
            for (Question question : questions) {
                List<AnswerWithFeedback> questionAnswers = answers.stream()
                    .filter(a -> a.getQuestion() != null && a.getQuestion().getId().equals(question.getId()))
                    .map(answer -> {
                        AnswerWithFeedback awf = new AnswerWithFeedback();
                        awf.setAnswer(answer);
                        
                        if (answer.getFeedbacks() != null) {
                            answer.getFeedbacks().forEach(feedback -> {
                                if (feedback.getFeedbackType() == Feedback.FeedbackType.AI) {
                                    awf.setAiFeedback(feedback);
                                } else if (feedback.getFeedbackType() == Feedback.FeedbackType.INTERVIEWER) {
                                    awf.setInterviewerFeedback(feedback);
                                }
                            });
                        }
                        
                        return awf;
                    })
                    .collect(Collectors.toList());
                
                answersByQuestion.put(question.getId(), questionAnswers);
            }
            
            long totalAnswerCount = answers.size();
            long answeredQuestionCount = answersByQuestion.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .count();
            
            log.info("✅ 리뷰 상세 로드 완료 - 질문: {}, 답변: {}", questions.size(), totalAnswerCount);
            
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("session", session);
            model.addAttribute("questions", questions);
            model.addAttribute("answersByQuestion", answersByQuestion);
            model.addAttribute("totalAnswerCount", totalAnswerCount);
            model.addAttribute("answeredQuestionCount", answeredQuestionCount);
            
            return "review/detail";
        } catch (Exception e) {
            log.error("❌ 리뷰 상세 로드 실패: {}", e.getMessage(), e);
            model.addAttribute("error", "리뷰를 불러오는데 실패했습니다: " + e.getMessage());
            return "redirect:/review/list";
        }
    }

    @GetMapping("/create")
    public String showReviewForm(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            model.addAttribute("currentUser", currentUser);
            return "review/create";
        } catch (Exception e) {
            log.error("❌ 리뷰 작성 페이지 로드 실패", e);
            return "redirect:/review/list";
        }
    }

    public static class AnswerWithFeedback {
        private Answer answer;
        private Feedback aiFeedback;
        private Feedback interviewerFeedback;

        public Answer getAnswer() { return answer; }
        public void setAnswer(Answer answer) { this.answer = answer; }
        
        public Feedback getAiFeedback() { return aiFeedback; }
        public void setAiFeedback(Feedback aiFeedback) { this.aiFeedback = aiFeedback; }
        
        public Feedback getInterviewerFeedback() { return interviewerFeedback; }
        public void setInterviewerFeedback(Feedback interviewerFeedback) { this.interviewerFeedback = interviewerFeedback; }
        
        public boolean hasAiFeedback() { return aiFeedback != null; }
        public boolean hasInterviewerFeedback() { return interviewerFeedback != null; }
    }
}