package com.mockerview.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MessagePartnerResponse {

    private String partnerUsername;
    private long   unreadCount;
    private String lastMessageContent;
    private LocalDateTime lastSentAt;

}
