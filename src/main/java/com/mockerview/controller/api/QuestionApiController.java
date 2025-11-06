package com.mockerview.controller.api;

import com.mockerview.entity.Question;
import com.mockerview.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Slf4j
public class QuestionApiController {
    private final QuestionRepository questionRepository;

    @GetMapping("/{sessionId}/questions")
    public ResponseEntity<?> getSessionQuestions(@PathVariable Long sessionId) {
        List<Question> questions = questionRepository.findBySessionId(sessionId);
        
        List<Map<String, Object>> questionList = questions.stream()
                .map(q -> Map.of(
                        "id", (Object) q.getId(),
                        "content", (Object) q.getContent(),
                        "questionOrder", (Object) q.getQuestionOrder()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(questionList);
    }

    @GetMapping("/{sessionId}/participants")
    public ResponseEntity<?> getSessionParticipants(
            @PathVariable Long sessionId,
            @RequestParam(required = false) String role) {
        
        List<Map<String, Object>> participants = List.of(
                Map.of("id", 1L, "name", "테스트 사용자", "role", "STUDENT")
        );

        return ResponseEntity.ok(participants);
    }
}
