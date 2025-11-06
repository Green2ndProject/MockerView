package com.mockerview.repository;

import com.mockerview.entity.InterviewerNote;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
