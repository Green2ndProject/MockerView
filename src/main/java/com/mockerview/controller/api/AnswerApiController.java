package com.mockerview.controller.api;

import com.mockerview.entity.Answer;
import com.mockerview.entity.Question;
import com.mockerview.entity.User;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.QuestionRepository;
import com.mockerview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/answers")
@RequiredArgsConstructor
@Slf4j
public class AnswerApiController {
        private final AnswerRepository answerRepository;
        private final QuestionRepository questionRepository;
        private final UserRepository userRepository;

        @PostMapping
        public ResponseEntity<?> saveAnswer(@RequestBody Map<String, Object> request) {
                try {
                Long sessionId = Long.valueOf(request.get("sessionId").toString());
                Long questionId = Long.valueOf(request.get("questionId").toString());
                Long userId = Long.valueOf(request.get("userId").toString());
                String content = request.get("content").toString();

                Question question = questionRepository.findById(questionId)
                        .orElseThrow(() -> new RuntimeException("Question not found"));

                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                Answer answer = Answer.builder()
                        .question(question)
                        .user(user)
                        .answerText(content)
                        .createdAt(LocalDateTime.now())
                        .build();

                answerRepository.save(answer);

                log.info("Answer saved - questionId: {}, userId: {}", questionId, userId);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "답변이 저장되었습니다."
                ));

                } catch (Exception e) {
                log.error("Failed to save answer", e);
                return ResponseEntity.status(500).body(Map.of(
                        "success", false,
                        "message", e.getMessage()
                ));
                }
        }
}
