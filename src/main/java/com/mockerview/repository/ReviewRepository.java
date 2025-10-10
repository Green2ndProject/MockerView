package com.mockerview.repository;

import com.mockerview.entity.Review;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findBySessionId(Long sessionId);
    @EntityGraph(attributePaths = {"session", "reviewer"})
    List<Review> findByReviewerId(Long reviewerId);
    List<Review> findByAnswerId(Long answerId);
}
