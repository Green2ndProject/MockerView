package com.mockerview.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Builder
public class ConversationSummaryDTO {

    private String partnerUsername;
    private String partnerName;
    private String lastMessageContent;
    private LocalDateTime lastMessageSentAt;
    private long unreadCount;

    public ConversationSummaryDTO(String partnerUsername, String partnerName, String content, LocalDateTime sentAt, long unreadCount){
        this.partnerUsername = partnerUsername;
        this.partnerName = partnerName;
        this.lastMessageContent = content;
        this.lastMessageSentAt = sentAt;
        this.unreadCount = unreadCount;
    }
}
