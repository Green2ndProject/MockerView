package com.mockerview.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockerview.entity.PrivateMessage;
import com.mockerview.entity.PrivateMessageStatus;

public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Long>{
    
    List<PrivateMessage> findBySenderUsernameAndReceiverUsernameOrReceiverUsernameAndSenderUsernameOrderBySentAtAsc(
        String userA_Sender, String userA_Receiver,
        String userB_Receiver, String userB_Sender        
    );

    Optional<PrivateMessage> findFirstBySenderUsernameAndReceiverUsernameOrReceiverUsernameAndSenderUsernameOrderBySentAtDesc(
        String user1, String user2, String user3, String user4
    );

    @Query("SELECT DISTINCT p.senderUsername FROM PrivateMessage p WHERE p.receiverUsername = :username " +
           "UNION " + 
           "SELECT DISTINCT p.receiverUsername FROM PrivateMessage p WHERE p.senderUsername = :username")
    List<String> findMyAllPartners(String username);

    @Query("SELECT p FROM PrivateMessage p "
         + "WHERE (p.senderUsername = :u1 AND p.receiverUsername = :u2) "
         + "OR (p.senderUsername = :u2 AND p.receiverUsername = :u1) " 
         + "ORDER BY p.sentAt DESC "
         + "LIMIT 1")
    Optional<PrivateMessage> findTopLatestMessage(
        @Param("u1") String user1, 
        @Param("u2") String user2);

    // 현재 사용자와 대화했던 모든 상대방의 고유한 username 목록을 조회하는 쿼리
    @Query("SELECT DISTINCT (CASE " +
           " WHEN pm.senderUsername = :currentUsername THEN pm.receiverUsername " +
           " ELSE pm.senderUsername " +
           "END) " +
           "FROM PrivateMessage pm " +
           "WHERE pm.senderUsername = :currentUsername OR pm.receiverUsername = :currentUsername") 
    List<String> findDistinctPartners(@Param("currentUsername") String currentUsername);

    @Query("SELECT COUNT(pm) FROM PrivateMessage pm " +
           "WHERE pm.receiverUsername = :currentUsername " + 
           "  AND pm.senderUsername = :partnerUsername " +
           "  AND pm.id > :lastReadMessageId")
	long countUnreadMessages(
        @Param("currentUsername") String currentUsername, 
        @Param("partnerUsername") String partnerUsername, 
        @Param("lastReadMessageId") Long lastReadMessageId);  
        
    @Query(value =
        "SELECT COALESCE(SUM(CASE WHEN pm.id > pms.last_read_message_id THEN 1 ELSE 0 END), 0) " +
        "FROM private_message pm " +
        "JOIN private_message_status pms " +
        "  ON pm.receiver_username = pms.user_username " +
        "  AND pm.sender_username = pms.partner_username " +
        "WHERE pms.user_username = :currentUsername " +
        "  AND pms.is_exited = FALSE " +
        "  AND pm.receiver_username = :currentUsername",
        nativeQuery = true)
    Long sumTotalUnreadMessagesByUser(@Param("currentUsername") String currentUsername);
}
