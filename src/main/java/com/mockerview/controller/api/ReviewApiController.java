package com.mockerview.controller.api;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.dto.ReviewDTO;
import com.mockerview.entity.Answer;
import com.mockerview.entity.Review;
import com.mockerview.entity.Session;
import com.mockerview.entity.User;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.ReviewRepository;
import com.mockerview.repository.SessionRepository;
import com.mockerview.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/review")
@Slf4j
public class ReviewApiController {

    private final ReviewRepository reviewRepository;
    private final SessionRepository sessionRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;

    public ReviewApiController(ReviewRepository reviewRepository,
                                SessionRepository sessionRepository,
                                AnswerRepository answerRepository,
                                UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.sessionRepository = sessionRepository;
        this.answerRepository = answerRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ResponseEntity<Map<String, Object>> createReview(
            @RequestBody ReviewRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        log.info("리뷰 생성 요청 - sessionId: {}, answerId: {}, rating: {}", 
            request.getSessionId(), request.getAnswerId(), request.getRating());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (request.getSessionId() == null) {
                response.put("success", false);
                response.put("message", "세션 ID가 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            User reviewer = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Session session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new RuntimeException("Session not found"));
            
            Review.ReviewBuilder reviewBuilder = Review.builder()
                .session(session)
                .reviewer(reviewer)
                .reviewComment(request.getComment())
                .rating(request.getRating());
            
            if (request.getAnswerId() != null && request.getAnswerId() > 0) {
                Answer answer = answerRepository.findById(request.getAnswerId())
                    .orElseThrow(() -> new RuntimeException("Answer not found"));
                reviewBuilder.answer(answer);
            }
            
            Review review = reviewBuilder.build();
            Review savedReview = reviewRepository.saveAndFlush(review);
            
            log.info("리뷰 생성 완료 - reviewId: {}", savedReview.getId());
            
            response.put("success", true);
            response.put("reviewId", savedReview.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("리뷰 생성 중 오류", e);
            response.put("success", false);
            response.put("message", "리뷰 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/session/{sessionId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ReviewDTO>> getReviewsBySession(@PathVariable Long sessionId) {
        List<Review> reviews = reviewRepository.findBySessionId(sessionId);
        
        List<ReviewDTO> dtos = reviews.stream()
            .map(r -> ReviewDTO.builder()
                .id(r.getId())
                .sessionId(r.getSession().getId())
                .answerId(r.getAnswer() != null ? r.getAnswer().getId() : null)
                .reviewerId(r.getReviewer().getId())
                .reviewerName(r.getReviewer().getName())
                .reviewComment(r.getReviewComment())
                .rating(r.getRating())
                .createdAt(r.getCreatedAt())
                .build())
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
}

class ReviewRequest {
    private Long sessionId;
    private Long answerId;
    private String comment;
    private Double rating;

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getAnswerId() { return answerId; }
    public void setAnswerId(Long answerId) { this.answerId = answerId; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
}