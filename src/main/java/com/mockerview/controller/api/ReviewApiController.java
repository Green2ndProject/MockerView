package com.mockerview.controller.api;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.dto.ReviewDTO;
import com.mockerview.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/review")
@Slf4j
public class ReviewApiController {

    private final ReviewService reviewService;

    public ReviewApiController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
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
            
            ReviewDTO reviewDTO;
            
            if (request.getAnswerId() != null && request.getAnswerId() > 0) {
                reviewDTO = reviewService.createReview(
                    request.getSessionId(),
                    request.getAnswerId(),
                    userDetails.getUserId(),
                    request.getComment(),
                    request.getRating()
                );
            } else {
                reviewDTO = reviewService.createSimpleReview(
                    request.getSessionId(),
                    userDetails.getUserId(),
                    request.getComment(),
                    request.getRating()
                );
            }
            
            log.info("리뷰 생성 완료 - reviewId: {}", reviewDTO.getId());
            
            response.put("success", true);
            response.put("reviewId", reviewDTO.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("리뷰 생성 중 오류", e);
            response.put("success", false);
            response.put("message", "리뷰 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsBySession(@PathVariable Long sessionId) {
        List<ReviewDTO> reviews = reviewService.getReviewsBySession(sessionId);
        return ResponseEntity.ok(reviews);
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