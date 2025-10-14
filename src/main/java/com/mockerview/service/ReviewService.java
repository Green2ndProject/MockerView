package com.mockerview.service;

import com.mockerview.dto.ReviewDTO;
import com.mockerview.entity.*;
import com.mockerview.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
        private final ReviewRepository reviewRepository;
        private final SessionRepository sessionRepository;
        private final AnswerRepository answerRepository;
        private final UserRepository userRepository;

        @Transactional(readOnly = true)
        public List<Session> getReviewableSessions() {
        List<Session> sessions = sessionRepository.findByStatusAndIsReviewable(Session.SessionStatus.ENDED, "Y");
        for (Session session : sessions) {
                if (session.getHost() != null) {
                session.getHost().getName();
                }
                session.getQuestions().size();
        }
        return sessions;
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
        log.info("리뷰 생성 완료 - reviewId: {}, sessionId: {}", review.getId(), sessionId);
        
        return convertToDTO(review);
        }

        @Transactional
        public ReviewDTO createSimpleReview(Long sessionId, Long reviewerId, String comment, Double rating) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new RuntimeException("Reviewer not found"));

        Review review = Review.builder()
                .session(session)
                .reviewer(reviewer)
                .reviewComment(comment)
                .rating(rating)
                .build();
        
        review = reviewRepository.save(review);
        log.info("간단 리뷰 생성 완료 - reviewId: {}, sessionId: {}", review.getId(), sessionId);
        
        return convertToDTO(review);
        }

        @Transactional(readOnly = true)
        public List<ReviewDTO> getReviewsBySession(Long sessionId) {
        return reviewRepository.findBySessionId(sessionId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<ReviewDTO> getReviewsByAnswer(Long answerId) {
        return reviewRepository.findByAnswerId(answerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<ReviewDTO> getReviewsByReviewer(Long reviewerId) {
        return reviewRepository.findByReviewerId(reviewerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public Session getSessionWithDetails(Long sessionId) {
        Session session = sessionRepository.findByIdWithHost(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        if (session.getHost() != null) {
                session.getHost().getName();
        }
        session.getQuestions().size();
        return session;
        }

        private ReviewDTO convertToDTO(Review review) {
        return ReviewDTO.builder()
                .id(review.getId())
                .sessionId(review.getSession().getId())
                .sessionTitle(review.getSession() != null ? review.getSession().getTitle() : null)
                .answerId(review.getAnswer() != null ? review.getAnswer().getId() : null)
                .reviewerId(review.getReviewer().getId())
                .reviewerName(review.getReviewer().getName())
                .reviewComment(review.getReviewComment())
                .rating(review.getRating())
                .createdAt(review.getCreatedAt())
                .build();
        }
}