package com.mockerview.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.mockerview.entity.PrivateMessage;

public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Long>{
    
    List<PrivateMessage> findBySenderUsernameAndReceiverUsernameOrReceiverUsernameAndSenderUsernameOrderBySentAtAsc(
        String userA_Sender, String userA_Receiver,
        String userB_Receiver, String userB_Sender        
    );
}
