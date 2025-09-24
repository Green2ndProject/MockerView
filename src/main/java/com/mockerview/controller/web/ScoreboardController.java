package com.mockerview.controller.web;

import com.mockerview.entity.*;
import com.mockerview.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/scoreboard")
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
        Optional<Session> sessionOpt = sessionRepository.findById(sessionId);
        if (!sessionOpt.isPresent()) {
            return "redirect:/session/list";
        }
        
        Session session = sessionOpt.get();
        List<Answer> answers = answerRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        
        if (answers.isEmpty()) {
            model.addAttribute("session", session);
            model.addAttribute("userScores", new ArrayList<>());
            model.addAttribute("totalQuestions", 0L);
            return "session/scoreboard";
        }
        
        Map<Long, List<Answer>> answersByUser = answers.stream()
                .collect(Collectors.groupingBy(answer -> answer.getUser().getId()));
        
        List<Map<String, Object>> userScores = new ArrayList<>();
        
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
            
            Map<String, Object> userScore = new HashMap<>();
            userScore.put("user", user);
            userScore.put("answerCount", userAnswers.size());
            userScore.put("avgAiScore", Math.round(avgAiScore * 10) / 10.0);
            userScore.put("avgInterviewerScore", Math.round(avgInterviewerScore * 10) / 10.0);
            userScore.put("totalScore", Math.round((avgAiScore + avgInterviewerScore) / 2 * 10) / 10.0);
            userScore.put("answers", userAnswers);
            
            userScores.add(userScore);
        }
        
        userScores.sort((a, b) -> Double.compare((Double) b.get("totalScore"), (Double) a.get("totalScore")));
        
        model.addAttribute("session", session);
        model.addAttribute("userScores", userScores);
        
        Long totalQuestions = questionRepository.countBySessionId(sessionId);
        model.addAttribute("totalQuestions", totalQuestions);
        
        return "session/scoreboard";
    }
}
