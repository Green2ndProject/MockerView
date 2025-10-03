package com.mockerview.controller.api;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.dto.ReviewDTO;
import com.mockerview.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewApiController {
    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewDTO> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long sessionId,
            @RequestParam Long answerId,
            @RequestParam String comment,
            @RequestParam Double rating) {
        ReviewDTO review = reviewService.createReview(sessionId, answerId, userDetails.getUserId(), comment, rating);
        return ResponseEntity.ok(review);
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<ReviewDTO>> getBySession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(reviewService.getReviewsBySession(sessionId));
    }

    @GetMapping("/answer/{answerId}")
    public ResponseEntity<List<ReviewDTO>> getByAnswer(@PathVariable Long answerId) {
        return ResponseEntity.ok(reviewService.getReviewsByAnswer(answerId));
    }
}
