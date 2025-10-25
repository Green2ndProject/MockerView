package com.mockerview.repository;

import com.mockerview.entity.InterviewMBTI;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewMBTIRepository extends JpaRepository<InterviewMBTI, Long> {
    
    @Query("SELECT m FROM InterviewMBTI m JOIN FETCH m.user WHERE m.user.id = :userId ORDER BY m.createdAt DESC")
    Optional<InterviewMBTI> findTopByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    @Query("SELECT m FROM InterviewMBTI m JOIN FETCH m.user WHERE m.user.id = :userId ORDER BY m.createdAt DESC")
    Optional<InterviewMBTI> findLatestByUserId(@Param("userId") Long userId);
    
    @Query("SELECT m FROM InterviewMBTI m JOIN FETCH m.user WHERE m.user.id = :userId ORDER BY m.createdAt DESC")
    List<InterviewMBTI> findByUserId(@Param("userId") Long userId);
}