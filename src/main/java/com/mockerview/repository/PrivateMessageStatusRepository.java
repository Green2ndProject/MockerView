package com.mockerview.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockerview.entity.PrivateMessageStatus;

public interface PrivateMessageStatusRepository extends JpaRepository<PrivateMessageStatus, Long>{
    
    Optional<PrivateMessageStatus> findByUserUsernameAndPartnerUsername(String userUsername, String partnerUsername);

    @Query("SELECT ms.partnerUsername FROM PrivateMessageStatus ms WHERE ms.userUsername = :username AND ms.isExited = FALSE")
    List<String> findActivePartnerUsernames(@Param("username") String username);
}
