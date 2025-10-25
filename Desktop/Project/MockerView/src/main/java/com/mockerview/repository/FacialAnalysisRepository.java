package com.mockerview.repository;

import com.mockerview.entity.FacialAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacialAnalysisRepository extends JpaRepository<FacialAnalysis, Long> {
    
    @Query("SELECT DISTINCT f FROM FacialAnalysis f " +
            "JOIN FETCH f.answer a " +
            "JOIN FETCH a.user " +
            "JOIN FETCH a.question " +
            "WHERE a.user.id = :userId " +
            "ORDER BY f.createdAt DESC")
    List<FacialAnalysis> findByAnswerUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}
