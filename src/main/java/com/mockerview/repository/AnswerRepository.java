package com.mockerview.repository;

import com.mockerview.entity.Answer;
import com.mockerview.entity.Question;
import com.mockerview.entity.Session;
import com.mockerview.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findByQuestionId(Long questionId);

    @Query("SELECT a FROM Answer a WHERE a.question.session = :session AND a.user = :answerer")
    List<Answer> findBySessionAndAnswerer(@Param("session") Session session, @Param("answerer") User answerer);

    @Query("SELECT a FROM Answer a JOIN FETCH a.feedbacks WHERE a.user.id = :userId")
    List<Answer> findByUserIdWithFeedbacks(@Param("userId") Long userId);

    List<Answer> findByQuestionIdOrderByCreatedAtAsc(Long questionId);

    List<Answer> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT a FROM Answer a WHERE a.question.session.id = :sessionId ORDER BY a.createdAt")
    List<Answer> findBySessionIdOrderByCreatedAt(@Param("sessionId") Long sessionId);

    @Query("SELECT a FROM Answer a WHERE a.question.session.id = :sessionId ORDER BY a.createdAt")
    List<Answer> findByQuestionSessionIdOrderByCreatedAt(@Param("sessionId") Long sessionId);

    @Query("SELECT a FROM Answer a WHERE a.question.session.id = :sessionId AND a.user.id = :userId")
    List<Answer> findByQuestionSessionIdAndUserId(@Param("sessionId") Long sessionId, @Param("userId") Long userId);

    long countByUserId(Long userId);

    @Query("SELECT DISTINCT a.question FROM Answer a " +
           "GROUP BY a.question " +
           "HAVING COUNT(a) >= :minCount")
    List<Question> findQuestionsWithMinAnswers(@Param("minCount") int minCount);

    @Query("SELECT COUNT(DISTINCT a.question) FROM Answer a")
    long countDistinctQuestions();

    @Query("SELECT COUNT(DISTINCT a.question) FROM Answer a " +
           "WHERE a.question.isAiGenerated = true")
    long countAiGeneratedQuestions();

    @Query("SELECT a FROM Answer a WHERE a.user.id = :userId AND a.videoUrl IS NOT NULL ORDER BY a.createdAt DESC")
    List<Answer> findByUserIdAndVideoUrlIsNotNull(@Param("userId") Long userId);
}
