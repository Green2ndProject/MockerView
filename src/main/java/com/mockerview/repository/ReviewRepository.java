package com.mockerview.repository;

import com.mockerview.entity.Review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findBySessionId(Long sessionId);
    @EntityGraph(attributePaths = {"session", "reviewer", "answer"})
    List<Review> findByReviewerId(Long reviewerId);
    @EntityGraph(attributePaths = {"session", "reviewer", "answer", "answer.question", "answer.question.session"})
    Page<Review> findByReviewerId(Long reviewerId, Pageable pageable);
    List<Review> findByAnswerId(Long answerId);
    boolean existsByReviewerIdAndAnswerId(Long reviewerId, Long answerId);
}