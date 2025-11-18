package com.mockerview.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.mockerview.entity.PrivateMessage;

public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Long>{
    
    List<PrivateMessage> findBySenderUsernameAndReceiverUsernameOrReceiverUsernameAndSenderUsernameOrderBySentAtAsc(
        String userA_Sender, String userA_Receiver,
        String userB_Receiver, String userB_Sender        
    );

    long countByReceiverUsernameAndIdGreaterThan(String receiverUsername, Long lastReadMessageId);

    Optional<PrivateMessage> findFirstBySenderUsernameAndReceiverUsernameOrReceiverUsernameAndSenderUsernameOrderByIdDesc(
        String user1, String user2, String user3, String user4
    );

    @Query("SELECT DISTINCT p.senderUsername FROM PrivateMessage p WHERE p.receiverUsername = :username " +
           "UNION " + 
           "SELECT DISTINCT p.receiverUsername FROM PrivateMessage p WHERE p.senderUsername = :username")
    List<String> findMyAllPartners(String username);

    Optional<PrivateMessage> findTopBySenderUsernameAndReceiverUsernameOrReceiverUsernameAndSenderUsernameOrderByIdDesc(
            String sender1, String receiver1, String sender2, String receiver2);
}
