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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/review")
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
    public ResponseEntity<Map<String, Object>> createReview(
            @RequestBody ReviewRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        User reviewer = userRepository.findById(userDetails.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        Session session = sessionRepository.findById(request.getSessionId())
            .orElseThrow(() -> new RuntimeException("Session not found"));
        
        Answer answer = null;
        if (request.getAnswerId() != null) {
            answer = answerRepository.findById(request.getAnswerId())
                .orElseThrow(() -> new RuntimeException("Answer not found"));
        }
        
        Review review = Review.builder()
            .session(session)
            .reviewer(reviewer)
            .answer(answer)
            .reviewComment(request.getComment())
            .rating(request.getRating())
            .build();
        
        reviewRepository.save(review);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("reviewId", review.getId());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/session/{sessionId}")
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