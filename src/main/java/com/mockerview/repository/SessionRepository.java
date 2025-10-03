package com.mockerview.repository;

import com.mockerview.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    
    @Query("SELECT s FROM Session s LEFT JOIN FETCH s.host ORDER BY s.createdAt DESC")
    List<Session> findByOrderByCreatedAtDesc();
    
    @Query("SELECT s FROM Session s LEFT JOIN FETCH s.host WHERE s.id = :id")
    Optional<Session> findByIdWithHost(@Param("id") Long id);
    
    @Query("SELECT s FROM Session s LEFT JOIN FETCH s.host WHERE s.host.id = :hostId")
    List<Session> findByHostId(@Param("hostId") Long hostId);
    
    @Query("SELECT s FROM Session s LEFT JOIN FETCH s.host WHERE s.status = :status ORDER BY s.createdAt DESC")
    List<Session> findByStatus(@Param("status") Session.SessionStatus status);
    
    @Query("SELECT s FROM Session s LEFT JOIN FETCH s.host LEFT JOIN FETCH s.questions ORDER BY s.createdAt DESC")
    List<Session> findAllWithHostAndQuestions();
    
    @Query("SELECT COUNT(DISTINCT s.host.id) FROM Session s WHERE s.id = :sessionId")
    Long countBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT s FROM Session s LEFT JOIN FETCH s.host " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR s.title LIKE %:keyword%) " +
            "AND (:status IS NULL OR :status = '' OR s.status = :status) " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'ASC' THEN s.createdAt END ASC, " +
            "CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'DESC' THEN s.createdAt END DESC, " +
            "CASE WHEN :sortBy = 'title' AND :sortOrder = 'ASC' THEN s.title END ASC, " +
            "CASE WHEN :sortBy = 'title' AND :sortOrder = 'DESC' THEN s.title END DESC")
    List<Session> searchSessions(@Param("keyword") String keyword, 
                                @Param("status") String status, 
                                @Param("sortBy") String sortBy, 
                                @Param("sortOrder") String sortOrder);

    @Query(value = "SELECT s FROM Session s LEFT JOIN FETCH s.host " +
                "WHERE (:keyword IS NULL OR :keyword = '' OR s.title LIKE %:keyword%) " +
                "AND (:status IS NULL OR s.status = :status) ", 
        countQuery = "SELECT COUNT(s) FROM Session s WHERE " +
                    "(:keyword IS NULL OR :keyword = '' OR s.title LIKE %:keyword%) " +
                    "AND (:status IS NULL OR s.status = :status) ")
    Page<Session> searchSessionsPageable(@Param("keyword") String keyword, 
                                        @Param("status") Session.SessionStatus status, 
                                        Pageable pageable);

    @Query(value = "SELECT s FROM Session s LEFT JOIN FETCH s.host", 
        countQuery = "SELECT COUNT(s) FROM Session s")
    Page<Session> findAllSessionsWithHost(Pageable pageable);
    
    @Query("SELECT s FROM Session s LEFT JOIN FETCH s.host WHERE s.host.id = :hostId AND s.sessionType = :sessionType ORDER BY s.createdAt DESC")
    List<Session> findByHostIdAndSessionType(@Param("hostId") Long hostId, @Param("sessionType") String sessionType);
    
    @Query("SELECT s FROM Session s LEFT JOIN FETCH s.host WHERE s.status = :status AND s.isReviewable = :isReviewable ORDER BY s.endTime DESC")
    List<Session> findByStatusAndIsReviewable(@Param("status") Session.SessionStatus status, @Param("isReviewable") String isReviewable);

    List<Session> findByHostIdAndSessionTypeOrderByCreatedAtDesc(Long hostId, String sessionType);
}