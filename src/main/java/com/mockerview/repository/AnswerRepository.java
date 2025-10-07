package com.mockerview.repository;

import com.mockerview.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    
    @Query("SELECT a FROM Answer a WHERE a.question.session.id = :sessionId ORDER BY a.createdAt ASC")
    List<Answer> findBySessionIdOrderByCreatedAtAsc(@Param("sessionId") Long sessionId);
    
    @Query("SELECT a FROM Answer a WHERE a.question.id = :questionId ORDER BY a.createdAt ASC")
    List<Answer> findByQuestionIdOrderByCreatedAtAsc(@Param("questionId") Long questionId);
    
    @Query("SELECT DISTINCT a.user.name FROM Answer a WHERE a.question.session.id = :sessionId")
    List<String> findDistinctUserNamesBySessionId(@Param("sessionId") Long sessionId);
    
    @Query("SELECT COUNT(a) FROM Answer a WHERE a.question.session.id = :sessionId")
    Long countBySessionId(@Param("sessionId") Long sessionId);
    
    @Query("SELECT a FROM Answer a WHERE a.question.session.id = :sessionId ORDER BY a.createdAt ASC")
    List<Answer> findByQuestionSessionIdOrderByCreatedAt(@Param("sessionId") Long sessionId);

    @Query("SELECT a FROM Answer a WHERE a.user.id = :userId ORDER BY a.createdAt DESC")
    List<Answer> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT DISTINCT a FROM Answer a " +
            "LEFT JOIN FETCH a.feedbacks f " +
            "LEFT JOIN FETCH a.question q " +
            "WHERE a.user.id = :userId " +
            "ORDER BY a.createdAt DESC")
    List<Answer> findByUserIdWithFeedbacks(@Param("userId") Long userId);

    List<Answer> findByQuestionSessionIdAndUserId(Long sessionId, Long userId);

    @Query("SELECT DISTINCT a FROM Answer a " +
        "LEFT JOIN FETCH a.feedbacks f " +
        "LEFT JOIN FETCH a.question q " +
        "WHERE q.session.id = :sessionId AND a.user.id = :userId " +
        "ORDER BY q.orderNo, a.createdAt")
    List<Answer> findByQuestionSessionIdAndUserIdWithFeedbacks(@Param("sessionId") Long sessionId, @Param("userId") Long userId);

    @Query("SELECT DISTINCT a FROM Answer a " +
        "LEFT JOIN FETCH a.feedbacks " +
        "LEFT JOIN FETCH a.question q " +
        "LEFT JOIN FETCH a.user " +
        "WHERE q.session.id = :sessionId " +
        "ORDER BY q.orderNo, a.createdAt")
List<Answer> findAllBySessionIdWithFeedbacks(@Param("sessionId") Long sessionId);
}