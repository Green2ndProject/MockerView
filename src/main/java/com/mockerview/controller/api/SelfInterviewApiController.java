package com.mockerview.controller.api;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.Answer;
import com.mockerview.entity.Feedback;
import com.mockerview.entity.Question;
import com.mockerview.entity.Session;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.FeedbackRepository;
import com.mockerview.repository.QuestionRepository;
import com.mockerview.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/selfinterview")
@RequiredArgsConstructor
@Slf4j
public class SelfInterviewApiController {

    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final FeedbackRepository feedbackRepository;

    @GetMapping(value = "/{sessionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getSessionData(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            log.info("셀프면접 데이터 조회 - sessionId: {}, userId: {}", 
                    sessionId, userDetails.getUserId());
            
            Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
            
            List<Question> questions = questionRepository.findBySessionIdOrderByOrderNoAsc(sessionId);
            
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("id", session.getId());
            sessionData.put("title", session.getTitle());
            sessionData.put("sessionType", session.getSessionType());
            sessionData.put("difficulty", session.getDifficulty());
            sessionData.put("category", session.getCategory());
            sessionData.put("status", session.getSessionStatus().name());
            
            List<Map<String, Object>> questionList = questions.stream()
                .map(q -> {
                    Map<String, Object> qMap = new HashMap<>();
                    qMap.put("id", q.getId());
                    qMap.put("text", q.getText());
                    qMap.put("orderNo", q.getOrderNo());
                    return qMap;
                })
                .collect(Collectors.toList());
            
            sessionData.put("questions", questionList);
            
            log.info("✅ 셀프면접 데이터 반환 - 질문 수: {}", questionList.size());
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(sessionData);
            
        } catch (Exception e) {
            log.error("셀프면접 데이터 조회 실패", e);
            return ResponseEntity.internalServerError()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getSelfInterviewList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "6") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            log.info("셀프면접 목록 조회 - userId: {}, page: {}", userDetails.getUserId(), page);
            
            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Session> sessionPage = sessionRepository.findSelfInterviewsByHostIdPageable(userDetails.getUserId(), pageable);
            
            List<Map<String, Object>> sessionList = sessionPage.getContent().stream()
                .map(session -> {
                    Map<String, Object> sessionMap = new HashMap<>();
                    sessionMap.put("id", session.getId());
                    sessionMap.put("title", session.getTitle());
                    sessionMap.put("sessionType", session.getSessionType());
                    sessionMap.put("createdAt", session.getCreatedAt());
                    
                    List<Question> questions = questionRepository.findBySessionIdOrderByOrderNoAsc(session.getId());
                    sessionMap.put("questionCount", questions.size());
                    
                    if (!questions.isEmpty()) {
                        List<Answer> allAnswers = questions.stream()
                            .flatMap(q -> answerRepository.findByQuestionIdOrderByCreatedAtAsc(q.getId()).stream())
                            .toList();
                        
                        if (!allAnswers.isEmpty()) {
                            List<Feedback> aiFeedbacks = feedbackRepository.findByAnswerInAndFeedbackType(
                                allAnswers, Feedback.FeedbackType.AI);
                            
                            if (!aiFeedbacks.isEmpty()) {
                                double avgScore = aiFeedbacks.stream()
                                    .filter(f -> f.getScore() != null)
                                    .mapToInt(Feedback::getScore)
                                    .average()
                                    .orElse(0.0);
                                sessionMap.put("avgScore", avgScore);
                            } else {
                                sessionMap.put("avgScore", null);
                            }
                        } else {
                            sessionMap.put("avgScore", null);
                        }
                    } else {
                        sessionMap.put("avgScore", null);
                    }
                    
                    return sessionMap;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", sessionList);
            response.put("totalElements", sessionPage.getTotalElements());
            response.put("totalPages", sessionPage.getTotalPages());
            response.put("currentPage", page);
            response.put("size", size);
            
            log.info("✅ 셀프면접 목록 반환 - 세션 수: {}", sessionList.size());
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
            
        } catch (Exception e) {
            log.error("셀프면접 목록 조회 실패", e);
            return ResponseEntity.internalServerError()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("error", e.getMessage()));
        }
    }
}
