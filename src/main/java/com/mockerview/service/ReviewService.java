package com.mockerview.service;

import com.mockerview.dto.ReviewDTO;
import com.mockerview.entity.*;
import com.mockerview.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final SessionRepository sessionRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;

    public List<Session> getReviewableSessions() {
        return sessionRepository.findByStatusAndIsReviewable(Session.SessionStatus.ENDED, "Y");
    }

    @Transactional
    public ReviewDTO createReview(Long sessionId, Long answerId, Long reviewerId, String comment, Double rating) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Answer not found"));
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new RuntimeException("Reviewer not found"));

        Review review = Review.builder()
                .session(session)
                .answer(answer)
                .reviewer(reviewer)
                .reviewComment(comment)
                .rating(rating)
                .build();
        review = reviewRepository.save(review);

        return convertToDTO(review);
    }

    public List<ReviewDTO> getReviewsBySession(Long sessionId) {
        return reviewRepository.findBySessionId(sessionId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ReviewDTO> getReviewsByAnswer(Long answerId) {
        return reviewRepository.findByAnswerId(answerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ReviewDTO convertToDTO(Review review) {
        return ReviewDTO.builder()
                .id(review.getId())
                .sessionId(review.getSession().getId())
                .answerId(review.getAnswer().getId())
                .reviewerId(review.getReviewer().getId())
                .reviewerName(review.getReviewer().getName())
                .reviewComment(review.getReviewComment())
                .rating(review.getRating())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
