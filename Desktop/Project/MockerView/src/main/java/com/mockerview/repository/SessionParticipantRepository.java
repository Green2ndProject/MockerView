package com.mockerview.repository;

import com.mockerview.entity.SessionParticipant;
import com.mockerview.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface SessionParticipantRepository extends JpaRepository<SessionParticipant, Long> {
    
    List<SessionParticipant> findBySessionId(Long sessionId);
    
    @Query("SELECT sp FROM SessionParticipant sp WHERE sp.session.id = :sessionId AND sp.isOnline = true")
    List<SessionParticipant> findOnlineParticipants(@Param("sessionId") Long sessionId);
    
    @Query("SELECT sp FROM SessionParticipant sp WHERE sp.session.id = :sessionId AND sp.user.id = :userId")
    Optional<SessionParticipant> findBySessionIdAndUserId(@Param("sessionId") Long sessionId, @Param("userId") Long userId);
    
    @Query("SELECT sp FROM SessionParticipant sp WHERE sp.session.id = :sessionId AND sp.role = :role AND sp.isOnline = true")
    List<SessionParticipant> findOnlineByRole(@Param("sessionId") Long sessionId, @Param("role") User.UserRole role);
}
