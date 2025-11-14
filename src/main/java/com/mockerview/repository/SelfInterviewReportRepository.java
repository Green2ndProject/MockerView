package com.mockerview.repository;

import com.mockerview.entity.SelfInterviewReport;
import com.mockerview.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SelfInterviewReportRepository extends JpaRepository<SelfInterviewReport, Long> {
    List<SelfInterviewReport> findByUserOrderByCreatedAtDesc(User user);
}
