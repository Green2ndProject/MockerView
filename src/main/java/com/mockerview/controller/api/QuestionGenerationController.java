package com.mockerview.controller.api;

import com.mockerview.entity.Category;
import com.mockerview.entity.Question;
import com.mockerview.entity.Session;
import com.mockerview.entity.User;
import com.mockerview.repository.CategoryRepository;
import com.mockerview.repository.SessionRepository;
import com.mockerview.repository.UserRepository;
import com.mockerview.service.AIQuestionGeneratorService;
import com.mockerview.service.DifficultyAdaptiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionGenerationController {

    private final AIQuestionGeneratorService aiQuestionGenerator;
    private final DifficultyAdaptiveService difficultyAdaptiveService;
    private final CategoryRepository categoryRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getCategories() {
        List<Category> categories = categoryRepository.findByIsActiveTrueOrderByDisplayOrder();
        return ResponseEntity.ok(categories);
    }
    
    @GetMapping("/categories/main")
    public ResponseEntity<List<Category>> getMainCategories() {
        List<Category> categories = categoryRepository.findAllMainCategories();
        return ResponseEntity.ok(categories);
    }
    
    @GetMapping("/categories/{parentCode}/sub")
    public ResponseEntity<List<Category>> getSubCategories(@PathVariable String parentCode) {
        List<Category> subCategories = categoryRepository.findSubCategoriesByParentCode(parentCode);
        return ResponseEntity.ok(subCategories);
    }

    @GetMapping("/categories/descriptions")
    public ResponseEntity<Map<String, String>> getCategoryDescriptions() {
        Map<String, String> descriptions = aiQuestionGenerator.getCategoryDescriptions();
        return ResponseEntity.ok(descriptions);
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateQuestion(
            @RequestParam Long sessionId,
            @RequestParam String categoryCode,
            @RequestParam(defaultValue = "TECHNICAL") String questionType,
            Authentication auth) {
        
        try {
            Session session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));

            User user = userRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            Question question = difficultyAdaptiveService.generateAdaptiveQuestion(
                    session, user, categoryCode, questionType
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("question", question);
            response.put("difficulty", question.getDifficultyLevel());
            response.put("difficultyDesc", difficultyAdaptiveService.getDifficultyDescription(question.getDifficultyLevel()));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("질문 생성 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "질문 생성에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/generate-simple")
    public ResponseEntity<?> generateSimpleQuestion(
            @RequestParam Long sessionId,
            @RequestParam String categoryCode,
            @RequestParam(defaultValue = "2") Integer difficulty,
            @RequestParam(defaultValue = "TECHNICAL") String questionType) {
        
        try {
            Session session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));

            Category category = categoryRepository.findByCode(categoryCode)
                    .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다."));

            Question question = aiQuestionGenerator.generateQuestion(
                    category, difficulty, questionType, session, null
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "question", question
            ));

        } catch (Exception e) {
            log.error("질문 생성 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/difficulty/{level}/description")
    public ResponseEntity<Map<String, String>> getDifficultyDescription(@PathVariable Integer level) {
        String description = difficultyAdaptiveService.getDifficultyDescription(level);
        return ResponseEntity.ok(Map.of("description", description));
    }

    @GetMapping("/types")
    public ResponseEntity<Map<String, String>> getQuestionTypes() {
        Map<String, String> types = Map.of(
                "TECHNICAL", "기술/전문 지식 질문",
                "BEHAVIORAL", "경험 기반 행동 질문 (STAR 기법)",
                "SITUATIONAL", "가상 상황 대처 질문",
                "PERSONALITY", "인성/가치관 질문"
        );
        return ResponseEntity.ok(types);
    }
}
