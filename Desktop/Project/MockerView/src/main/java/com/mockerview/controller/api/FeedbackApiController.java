package com.mockerview.controller.api;

import com.mockerview.entity.Answer;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.service.AIFeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@Slf4j
public class FeedbackApiController {

    private final AIFeedbackService aiFeedbackService;
    private final AnswerRepository answerRepository;

    @PostMapping("/ai/{answerId}")
    public ResponseEntity<Map<String, Object>> generateAIFeedback(@PathVariable Long answerId) {
        try {
            Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Answer not found"));
            
            Map<String, Object> feedback = aiFeedbackService.generateFeedbackSync(
                answer.getQuestion().getText(),
                answer.getAnswerText()
            );
            
            log.info("AI 피드백 생성 완료 - answerId: {}", answerId);
            return ResponseEntity.ok(feedback);
            
        } catch (Exception e) {
            log.error("AI 피드백 생성 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}