package com.mockerview.controller.api;

import com.mockerview.controller.websocket.SessionEndHandler;
import com.mockerview.dto.SelfInterviewCreateDTO;
import com.mockerview.entity.*;
import com.mockerview.repository.CategoryRepository;
import com.mockerview.repository.SessionRepository;
import com.mockerview.repository.UserRepository;
import com.mockerview.service.UnifiedQuestionGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/selfinterview")
@RequiredArgsConstructor
public class SelfInterviewApiController {

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final UnifiedQuestionGeneratorService questionGenerator;
    private final SessionEndHandler sessionEndHandler;

    @PostMapping("/create-with-ai")
    public ResponseEntity<?> createWithAI(
            @RequestBody SelfInterviewCreateDTO dto,
            Authentication auth) {
        
        try {
            log.info("📥 AI 셀프면접 생성 요청 - 제목: {}, 카테고리: {}, 난이도: {}, 질문수: {}", 
                    dto.getTitle(), dto.getCategory(), dto.getDifficulty(), dto.getQuestionCount());
            
            User user = userRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            Category category = categoryRepository.findByCode(dto.getCategory())
                    .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다."));

            Session session = Session.builder()
                    .host(user)
                    .title(dto.getTitle())
                    .sessionType(dto.getSessionType() != null ? dto.getSessionType() : "TEXT")
                    .difficulty(dto.getDifficulty() != null ? dto.getDifficulty() : "MEDIUM")
                    .category(dto.getCategory())
                    .isSelfInterview("Y")
                    .isReviewable("Y")
                    .aiEnabled(true)
                    .aiMode("FULL")
                    .sessionStatus(Session.SessionStatus.PLANNED)
                    .createdAt(LocalDateTime.now())
                    .lastActivity(LocalDateTime.now())
                    .build();

            session = sessionRepository.save(session);
            log.info("✅ 세션 생성 완료 - sessionId: {}", session.getId());

            int questionCount = dto.getQuestionCount() != null ? dto.getQuestionCount() : 5;
            String questionType = dto.getQuestionType() != null ? dto.getQuestionType() : "TECHNICAL";
            int difficultyLevel = dto.getDifficultyLevel() != null ? dto.getDifficultyLevel() : 2;

            log.info("🔄 질문 생성 시작 - 총 {}개", questionCount);
            
            for (int i = 0; i < questionCount; i++) {
                Question question = questionGenerator.generateQuestion(
                        category,
                        difficultyLevel,
                        questionType,
                        session
                );
                
                question.setQuestioner(user);
                question.setOrderNo(i + 1);
                session.getQuestions().add(question);
                
                log.info("✅ 질문 {}/{} 생성 완료", i + 1, questionCount);
            }

            sessionRepository.save(session);
            
            log.info("🎉 AI 셀프면접 생성 완료 - sessionId: {}, 질문수: {}", 
                    session.getId(), session.getQuestions().size());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sessionId", session.getId());
            response.put("questionCount", session.getQuestions().size());
            response.put("message", "AI 면접이 생성되었습니다!");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ AI 면접 생성 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "면접 생성에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<?> completeSelfInterview(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal User user
    ) {
        try {
            Session session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다"));

            if (!session.getHost().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "권한이 없습니다"));
            }

            session.setStatus(Session.SessionStatus.ENDED);
            sessionRepository.save(session);

            log.info("✅ 셀프 면접 완료: sessionId={}", sessionId);

            sessionEndHandler.handleSessionEnd(sessionId);

            return ResponseEntity.ok(Map.of(
                    "message", "셀프 면접이 완료되었습니다",
                    "sessionId", sessionId,
                    "status", "ENDED"
            ));
        } catch (Exception e) {
            log.error("셀프 면접 완료 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
