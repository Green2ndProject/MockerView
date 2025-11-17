package com.mockerview.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mockerview.entity.PrivateMessageStatus;

public interface PrivateMessageStatusRepository extends JpaRepository<PrivateMessageStatus, Long>{
    
    Optional<PrivateMessageStatus> findByUserUsernameAndPartnerUsername(String userUsername, String partnerUsername);
}
