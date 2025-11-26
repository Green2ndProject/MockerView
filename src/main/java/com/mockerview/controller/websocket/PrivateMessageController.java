package com.mockerview.controller.websocket;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

import com.mockerview.dto.PrivateMessageRequest;
import com.mockerview.dto.ReadReceiptRequest;
import com.mockerview.service.PrivateMessageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequiredArgsConstructor
public class PrivateMessageController {
    
    private final PrivateMessageService privateMessageService;

    @MessageMapping("/private/message")
    public void sendPrivateMessage(
        Principal principal, 
        PrivateMessageRequest request
    ){
       String senderUsername = principal.getName();

       privateMessageService.saveAndSend(senderUsername, request);
    }

    @MessageMapping("/messages/read")
    public void handleReadReceipt(
        Principal principal, 
        @Payload ReadReceiptRequest request) {
    
    String currentUsername = principal.getName();
    String partnerUsername = request.getPartnerUsername();

    privateMessageService.markMessageAsRead(currentUsername, partnerUsername);
    
    }
}

