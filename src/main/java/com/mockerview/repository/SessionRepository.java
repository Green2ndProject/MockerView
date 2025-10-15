package com.mockerview.repository;

import com.mockerview.entity.Session;
import com.mockerview.entity.Session.SessionStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
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
    
    @Query("SELECT s FROM Session s LEFT JOIN FETCH s.host WHERE s.sessionStatus = :status ORDER BY s.createdAt DESC")
    List<Session> findByStatus(@Param("status") Session.SessionStatus status);
    
    @Query("SELECT s FROM Session s LEFT JOIN FETCH s.host LEFT JOIN FETCH s.questions ORDER BY s.createdAt DESC")
    List<Session> findAllWithHostAndQuestions();
    
    @Query("SELECT COUNT(DISTINCT s.host.id) FROM Session s WHERE s.id = :sessionId")
    Long countBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT s FROM Session s LEFT JOIN FETCH s.host " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR s.title LIKE %:keyword%) " +
            "AND (:status IS NULL OR :status = '' OR s.sessionStatus = :status) " +
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
                "WHERE s.isSelfInterview = 'N' " +
                "AND (:keyword IS NULL OR :keyword = '' OR s.title LIKE %:keyword%) " +
                "AND (:status IS NULL OR s.sessionStatus = :status) ", 
        countQuery = "SELECT COUNT(s) FROM Session s WHERE s.isSelfInterview = 'N' " +
                    "AND (:keyword IS NULL OR :keyword = '' OR s.title LIKE %:keyword%) " +
                    "AND (:status IS NULL OR s.sessionStatus = :status) ")
    Page<Session> searchSessionsPageable(@Param("keyword") String keyword, 
                                        @Param("status") Session.SessionStatus status, 
                                        Pageable pageable);

    @Query(value = "SELECT s FROM Session s LEFT JOIN FETCH s.host WHERE s.isSelfInterview = 'N'", 
        countQuery = "SELECT COUNT(s) FROM Session s WHERE s.isSelfInterview = 'N'")
    Page<Session> findAllSessionsWithHost(Pageable pageable);
    
    @Query("SELECT s FROM Session s LEFT JOIN FETCH s.host WHERE s.host.id = :hostId AND s.sessionType = :sessionType ORDER BY s.createdAt DESC")
    List<Session> findByHostIdAndSessionType(@Param("hostId") Long hostId, @Param("sessionType") String sessionType);
    
    @Query("SELECT s FROM Session s LEFT JOIN FETCH s.host WHERE s.sessionStatus = :status AND s.isReviewable = :isReviewable ORDER BY s.endTime DESC")
    List<Session> findByStatusAndIsReviewable(@Param("status") Session.SessionStatus status, @Param("isReviewable") String isReviewable);

    List<Session> findByHostIdAndSessionTypeOrderByCreatedAtDesc(Long hostId, String sessionType);

    @Query("SELECT s FROM Session s LEFT JOIN FETCH s.host LEFT JOIN FETCH s.questions WHERE s.id = :id")
    Optional<Session> findByIdWithHostAndQuestions(@Param("id") Long id);

    @Query("SELECT s FROM Session s WHERE s.sessionStatus = :status AND (s.startTime IS NULL OR s.startTime <= :now)")
    List<Session> findByStatusAndStartTimeBefore(
        @Param("status") Session.SessionStatus status,
        @Param("now") LocalDateTime now
    );

    @Query("SELECT s FROM Session s WHERE s.sessionStatus = :status AND s.expiresAt < :now")
    List<Session> findExpiredSessions(
        @Param("status") Session.SessionStatus status, 
        @Param("now") LocalDateTime now
    );
    
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
            "FROM Session s WHERE s.id = :sessionId AND s.host.id = :hostId")
    boolean isHost(@Param("sessionId") Long sessionId, @Param("hostId") Long hostId);
    
    @Query(value = "SELECT s FROM Session s LEFT JOIN FETCH s.host " +
                "WHERE s.isSelfInterview = 'N' AND s.sessionStatus = :status",
        countQuery = "SELECT COUNT(s) FROM Session s " +
                    "WHERE s.isSelfInterview = 'N' AND s.sessionStatus = :status")
    Page<Session> findByStatusPageable(@Param("status") Session.SessionStatus status, Pageable pageable);
    
    Long countBySessionStatus(Session.SessionStatus status);

    @Query(value = "SELECT s FROM Session s LEFT JOIN FETCH s.host " +
               "WHERE s.sessionStatus = :status AND s.isReviewable = :isReviewable " +
               "ORDER BY s.endTime DESC", 
    countQuery = "SELECT COUNT(s) FROM Session s " +
                  "WHERE s.sessionStatus = :status AND s.isReviewable = :isReviewable")
    Page<Session> findByStatusAndIsReviewablePageable(
                    @Param("status") Session.SessionStatus status, 
                    @Param("isReviewable") String isReviewable,
                    Pageable pageable);

    @Query(value = "SELECT s FROM Session s LEFT JOIN FETCH s.host " +
               "WHERE s.host.id = :hostId AND s.isSelfInterview = :isSelfInterview " +
               "ORDER BY s.createdAt DESC", 
    countQuery = "SELECT COUNT(s) FROM Session s " +
                  "WHERE s.host.id = :hostId AND s.isSelfInterview = :isSelfInterview")
    Page<Session> findByHostIdAndIsSelfInterviewPageable(
                    @Param("hostId") Long hostId, 
                    @Param("isSelfInterview") String isSelfInterview,
                    Pageable pageable);
    @Query("SELECT s FROM Session s LEFT JOIN FETCH s.host WHERE s.host.id = :hostId AND s.isSelfInterview = 'Y' ORDER BY s.createdAt DESC")
    List<Session> findSelfInterviewsByHostId(@Param("hostId") Long hostId);

    @Query("SELECT COUNT(s) FROM Session s WHERE s.isSelfInterview = 'N'")
    Long countNonSelfInterviewSessions();

    @Query("SELECT COUNT(s) FROM Session s WHERE s.sessionStatus = :status AND s.isSelfInterview = :isSelfInterview")
    Long countBySessionStatusAndIsSelfInterview(@Param("status") SessionStatus status, @Param("isSelfInterview") String isSelfInterview);
}