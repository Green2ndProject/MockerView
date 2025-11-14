package com.mockerview.repository;

import com.mockerview.entity.InterviewReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewReportRepository extends JpaRepository<InterviewReport, Long> {
    
    @Query("SELECT r FROM InterviewReport r WHERE r.session.id = :sessionId ORDER BY r.createdAt DESC")
    List<InterviewReport> findBySessionId(@Param("sessionId") Long sessionId);
    
    @Query("SELECT r FROM InterviewReport r WHERE r.session.id = :sessionId AND r.status = 'COMPLETED' ORDER BY r.createdAt DESC")
    Optional<InterviewReport> findLatestCompletedBySessionId(@Param("sessionId") Long sessionId);
    
    @Query("SELECT r FROM InterviewReport r WHERE r.generatedBy.id = :userId ORDER BY r.createdAt DESC")
    List<InterviewReport> findByGeneratedByUserId(@Param("userId") Long userId);
    
    @Query("SELECT r FROM InterviewReport r WHERE r.status = :status")
    List<InterviewReport> findByStatus(@Param("status") InterviewReport.ReportStatus status);
    
    @Query("SELECT COUNT(r) FROM InterviewReport r WHERE r.session.id = :sessionId")
    long countBySessionId(@Param("sessionId") Long sessionId);
}
