package com.mockerview.repository;

import com.mockerview.entity.InterviewerNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewerNoteRepository extends JpaRepository<InterviewerNote, Long> {
    List<InterviewerNote> findBySessionId(Long sessionId);
    
    List<InterviewerNote> findBySessionIdAndInterviewerId(Long sessionId, Long interviewerId);
    
    Optional<InterviewerNote> findBySessionIdAndInterviewerIdAndIntervieweeId(
        Long sessionId, Long interviewerId, Long intervieweeId);
    
    List<InterviewerNote> findByIntervieweeIdAndSubmitted(Long intervieweeId, Boolean submitted);
    
    List<InterviewerNote> findByInterviewerIdAndSubmitted(Long interviewerId, Boolean submitted);
    
    @Query("SELECT AVG(n.rating) FROM InterviewerNote n WHERE n.interviewer.id = :interviewerId AND n.submitted = true AND n.rating IS NOT NULL")
    Double findAverageRatingByInterviewerId(@Param("interviewerId") Long interviewerId);
    
    @Query("SELECT n.interviewee.id, n.interviewee.name, AVG(n.rating) " +
            "FROM InterviewerNote n " +
            "WHERE n.interviewer.id = :interviewerId AND n.submitted = true AND n.rating IS NOT NULL " +
            "GROUP BY n.interviewee.id, n.interviewee.name " +
            "ORDER BY AVG(n.rating) DESC")
    List<Object[]> findTopIntervieweesByInterviewerId(@Param("interviewerId") Long interviewerId);
    
    @Query("SELECT n FROM InterviewerNote n " +
            "JOIN FETCH n.session " +
            "JOIN FETCH n.interviewer " +
            "WHERE n.interviewee.id = :intervieweeId AND n.submitted = true " +
            "ORDER BY n.createdAt DESC")
    List<InterviewerNote> findSubmittedFeedbacksForInterviewee(@Param("intervieweeId") Long intervieweeId);
}