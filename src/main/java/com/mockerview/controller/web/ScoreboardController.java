package com.mockerview.controller.web;

import com.mockerview.entity.*;
import com.mockerview.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
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
    public String scoreboard(@PathVariable Long sessionId, Model model) {
        try {
            Optional<Session> sessionOpt = sessionRepository.findById(sessionId);
            if (!sessionOpt.isPresent()) {
                log.warn("세션을 찾을 수 없음: {}", sessionId);
                return "redirect:/session/list";
            }
            
            Session interviewSession = sessionOpt.get();
            List<Answer> answers = answerRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
            
            if (answers.isEmpty()) {
                model.addAttribute("interviewSession", interviewSession);
                model.addAttribute("userScores", new ArrayList<>());
                model.addAttribute("totalQuestions", 0L);
                log.info("답변이 없어서 빈 스코어보드 반환");
                return "session/scoreboard";
            }
            
            Map<Long, List<Answer>> answersByUser = answers.stream()
                    .collect(Collectors.groupingBy(answer -> answer.getUser().getId()));
            
            List<UserScoreDTO> userScores = new ArrayList<>();
            
            for (Map.Entry<Long, List<Answer>> entry : answersByUser.entrySet()) {
                User user = entry.getValue().get(0).getUser();
                List<Answer> userAnswers = entry.getValue();
                
                List<Feedback> aiFeedbacks = feedbackRepository.findByAnswerInAndFeedbackType(userAnswers, Feedback.FeedbackType.AI);
                List<Feedback> interviewerFeedbacks = feedbackRepository.findByAnswerInAndFeedbackType(userAnswers, Feedback.FeedbackType.INTERVIEWER);
                
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
                dto.setTotalScore(Math.round((avgAiScore + avgInterviewerScore) / 2 * 10) / 10.0);
                dto.setAnswers(userAnswers);
                
                userScores.add(dto);
            }
            
            userScores.sort((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()));
            
            model.addAttribute("interviewSession", interviewSession);
            model.addAttribute("userScores", userScores);
            
            Long totalQuestions = questionRepository.countBySessionId(sessionId);
            model.addAttribute("totalQuestions", totalQuestions);
            
            log.info("스코어보드 로드 완료 - sessionId: {}, 참가자: {}명", sessionId, userScores.size());
            
            return "session/scoreboard";
            
        } catch (Exception e) {
            log.error("스코어보드 로드 오류: ", e);
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