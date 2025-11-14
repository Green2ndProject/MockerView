package com.mockerview.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "private_message")
public class PrivateMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "sender_username")
    private String senderUsername;
    @Column(name = "receiver_username")
    private String receiverUsername;

    @Column(columnDefinition = "text")
    private String content;
    @Column(name = "sent_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, 
                pattern = "yyyy-MM-dd'T'HH:mm:ss", 
                timezone = "Asia/Seoul")
    private LocalDateTime sentAt;
    @Column(name = "is_read")
    private boolean isRead;

    @Builder
    public PrivateMessage(String senderUsername, String receiverUsername, String content){
        this.senderUsername = senderUsername;
        this.receiverUsername = receiverUsername;
        this.content = content;
        this.sentAt = LocalDateTime.now();
        this.isRead = false;
    }
}
