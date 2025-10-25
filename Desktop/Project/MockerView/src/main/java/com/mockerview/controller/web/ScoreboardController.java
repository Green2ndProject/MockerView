package com.mockerview.controller.web;

import com.mockerview.entity.*;
import com.mockerview.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/session/scoreboard")
@Slf4j
public class ScoreboardController {

    @Autowired
    private SessionRepository sessionRepository;
    
    @Autowired
    private AnswerRepository answerRepository;
    
    @Autowired
    private FeedbackRepository feedbackRepository;
    
    @Autowired
    private QuestionRepository questionRepository;

    @GetMapping("/{sessionId}")
    @Transactional(readOnly = true)
    public String scoreboard(@PathVariable Long sessionId, Model model) {
        try {
            Session interviewSession = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없음: " + sessionId));
            
            List<Answer> answers = answerRepository.findByQuestionSessionIdOrderByCreatedAt(sessionId);
            
            if (answers.isEmpty()) {
                model.addAttribute("interviewSession", interviewSession);
                model.addAttribute("userScores", new ArrayList<>());
                model.addAttribute("totalQuestions", 0L);
                return "session/scoreboard";
            }
            
            List<Feedback> allFeedbacks = feedbackRepository.findByAnswerIn(answers);
            Map<Long, List<Feedback>> feedbacksByAnswerId = allFeedbacks.stream()
                    .collect(Collectors.groupingBy(f -> f.getAnswer().getId()));
            
            answers.forEach(answer -> {
                List<Feedback> answerFeedbacks = feedbacksByAnswerId.getOrDefault(answer.getId(), new ArrayList<>());
                answer.setFeedbacks(answerFeedbacks);
            });
            
            Map<Long, List<Answer>> answersByUser = answers.stream()
                    .filter(a -> a.getUser() != null)
                    .collect(Collectors.groupingBy(answer -> answer.getUser().getId()));
            
            List<UserScoreDTO> userScores = new ArrayList<>();
            
            for (Map.Entry<Long, List<Answer>> entry : answersByUser.entrySet()) {
                List<Answer> userAnswers = entry.getValue();
                if (userAnswers.isEmpty()) continue;
                
                User user = userAnswers.get(0).getUser();
                if (user == null) continue;
                
                List<Feedback> aiFeedbacks = feedbackRepository.findByAnswerInAndFeedbackType(
                    userAnswers, Feedback.FeedbackType.AI);
                List<Feedback> interviewerFeedbacks = feedbackRepository.findByAnswerInAndFeedbackType(
                    userAnswers, Feedback.FeedbackType.INTERVIEWER);
                
                double avgAiScore = aiFeedbacks.stream()
                        .filter(f -> f.getScore() != null)
                        .mapToDouble(Feedback::getScore)
                        .average()
                        .orElse(0.0);
                
                double avgInterviewerScore = interviewerFeedbacks.stream()
                        .filter(f -> f.getScore() != null)
                        .mapToDouble(Feedback::getScore)
                        .average()
                        .orElse(0.0);
                
                UserScoreDTO dto = new UserScoreDTO();
                dto.setUser(user);
                dto.setAnswerCount(userAnswers.size());
                dto.setAvgAiScore(Math.round(avgAiScore * 10) / 10.0);
                dto.setAvgInterviewerScore(Math.round(avgInterviewerScore * 10) / 10.0);
                
                double totalScore;
                if ("Y".equals(interviewSession.getIsSelfInterview())) {
                    totalScore = avgAiScore;
                } else {
                    totalScore = (avgAiScore + avgInterviewerScore) / 2;
                }
                dto.setTotalScore(Math.round(totalScore * 10) / 10.0);
                dto.setAnswers(userAnswers);
                
                userScores.add(dto);
            }
            
            userScores.sort((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()));
            
            model.addAttribute("interviewSession", interviewSession);
            model.addAttribute("userScores", userScores);
            
            Long totalQuestions = questionRepository.countBySessionId(sessionId);
            model.addAttribute("totalQuestions", totalQuestions);
            
            return "session/scoreboard";
            
        } catch (Exception e) {
            log.error("스코어보드 로드 오류: ", e);
            model.addAttribute("error", "스코어보드를 불러올 수 없습니다");
            return "redirect:/session/list";
        }
    }
    
    @lombok.Data
    public static class UserScoreDTO {
        private User user;
        private int answerCount;
        private double avgAiScore;
        private double avgInterviewerScore;
        private double totalScore;
        private List<Answer> answers;
    }
}