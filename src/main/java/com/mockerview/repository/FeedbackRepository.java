package com.mockerview.repository;

import com.mockerview.entity.Answer;
import com.mockerview.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    
    List<Feedback> findByAnswer(Answer answer);
    List<Feedback> findByAnswerIn(List<Answer> answers);
    
    @Query("SELECT f FROM Feedback f WHERE f.answer IN :answers AND f.feedbackType = :feedbackType")
    List<Feedback> findByAnswerInAndFeedbackType(@Param("answers") List<Answer> answers, 
                                                 @Param("feedbackType") Feedback.FeedbackType feedbackType);
}
