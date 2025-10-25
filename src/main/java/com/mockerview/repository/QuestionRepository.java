package com.mockerview.repository;

import com.mockerview.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    
    @Query("SELECT DISTINCT q FROM Question q LEFT JOIN FETCH q.answers WHERE q.session.id = :sessionId ORDER BY q.orderNo ASC")
    List<Question> findBySessionIdOrderByOrderNoAsc(@Param("sessionId") Long sessionId);
    
    @Query("SELECT DISTINCT q FROM Question q LEFT JOIN FETCH q.answers WHERE q.session.id = :sessionId ORDER BY q.orderNo ASC")
    List<Question> findBySessionIdOrderByOrderNo(@Param("sessionId") Long sessionId);
    
    @Query("SELECT q FROM Question q WHERE q.session.id = :sessionId ORDER BY q.orderNo ASC")
    List<Question> findBySessionId(@Param("sessionId") Long sessionId);
    
    @Query("SELECT COUNT(q) FROM Question q WHERE q.session.id = :sessionId")
    Long countBySessionId(@Param("sessionId") Long sessionId);
    
    @Query("SELECT q FROM Question q WHERE q.aiPromptHash = :hash")
    Optional<Question> findByAiPromptHash(@Param("hash") String hash);
    
    @Query("SELECT q FROM Question q WHERE q.category.id = :categoryId AND q.difficultyLevel = :difficulty AND q.isAiGenerated = false")
    List<Question> findManualQuestionsByCategoryAndDifficulty(@Param("categoryId") Long categoryId, @Param("difficulty") Integer difficulty);
}