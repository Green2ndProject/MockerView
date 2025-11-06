package com.mockerview.repository;

import com.mockerview.entity.VoiceAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoiceAnalysisRepository extends JpaRepository<VoiceAnalysis, Long> {
    
    @Query("SELECT DISTINCT v FROM VoiceAnalysis v " +
           "JOIN FETCH v.answer a " +
           "JOIN FETCH a.user " +
           "JOIN FETCH a.question " +
           "WHERE a.user.id = :userId " +
           "ORDER BY v.createdAt DESC")
    List<VoiceAnalysis> findByAnswerUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}
