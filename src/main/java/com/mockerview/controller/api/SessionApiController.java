package com.mockerview.controller.api;

import com.mockerview.entity.Answer;
import com.mockerview.entity.Question;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/session")
public class SessionApiController {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @GetMapping("/{sessionId}/questions")
    public ResponseEntity<List<Question>> getSessionQuestions(@PathVariable Long sessionId) {
        List<Question> questions = questionRepository.findBySessionIdOrderByOrderNoAsc(sessionId);
        return ResponseEntity.ok(questions);
    }

    @GetMapping("/{sessionId}/answers")
    public ResponseEntity<List<Answer>> getSessionAnswers(@PathVariable Long sessionId) {
        List<Answer> answers = answerRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return ResponseEntity.ok(answers);
    }
}
