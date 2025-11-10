package com.mockerview.repository;

import com.mockerview.entity.InterviewReport;
import com.mockerview.entity.Session;
import com.mockerview.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewReportRepository extends JpaRepository<InterviewReport, Long> {
    Optional<InterviewReport> findBySession(Session session);
    List<InterviewReport> findByUserOrderByCreatedAtDesc(User user);
    List<InterviewReport> findTop10ByUserOrderByCreatedAtDesc(User user);
    boolean existsBySession(Session session);
}
