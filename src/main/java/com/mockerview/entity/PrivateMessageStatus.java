package com.mockerview.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "private_message_status")     
public class PrivateMessageStatus {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_username", nullable = false)
    private String userUsername;

    @Column(name = "partner_username", nullable = false)
    private String partnerUsername;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(name = "is_exited")
    private boolean isExited = false; 

    @Builder
    public PrivateMessageStatus(String userUsername, String partnerUsername, Long lastReadMessageId){

        this.userUsername = userUsername;
        this.partnerUsername = partnerUsername;
        this.lastReadMessageId = (lastReadMessageId != null) ? lastReadMessageId : 0L;
    }

    public void updateLastReadMessageId(Long messageId){

        this.lastReadMessageId = messageId;
    }

    public void setIsExited(boolean isExited){
        
        this.isExited = isExited;
    }
}
