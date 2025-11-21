package com.mockerview.controller.api;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockerview.dto.PrivateMessageResponse;
import com.mockerview.dto.ConversationSummaryDTO;
import com.mockerview.service.PrivateMessageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PutMapping;



@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/private/messages")
public class PrivateMessageRestController {
    
    private final PrivateMessageService privateMessageService;

    @GetMapping("/{targetUsername}")
    public ResponseEntity<List<PrivateMessageResponse>> getMessageHistory(
        Principal principal, @PathVariable String targetUsername){

       String myUsername = principal.getName();

       log.info(myUsername);

       List<PrivateMessageResponse> history = privateMessageService.getMessageHistory(myUsername, targetUsername);
       
       return ResponseEntity.ok(history);
    }

    // 메시지 읽음 표시
    @PutMapping("/read/{targetUsername}")
    public ResponseEntity<Void> markAsRead(
            @PathVariable("targetUsername") String partnerUsername, 
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String currentUsername = userDetails.getUsername();

        privateMessageService.markMessageAsRead(currentUsername, partnerUsername);

        return ResponseEntity.ok().build();
    }

    // @GetMapping("/partners")
    // public ResponseEntity<List<MessagePartnerResponse>> getMessagePartners(@AuthenticationPrincipal UserDetails userDetails) {
        
    //     String currentUsername = userDetails.getUsername();

    //     List<MessagePartnerResponse> partners = privateMessageService.getMessagePartnerWithUnreadCount(currentUsername);

    //     return ResponseEntity.ok(partners);
    // }
    

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationSummaryDTO>> getConversations(
        @AuthenticationPrincipal UserDetails userDetails
    ){
        String currentUsername = userDetails.getUsername();

        List<ConversationSummaryDTO> conversations = privateMessageService.getConversationSummaries(currentUsername);

        return ResponseEntity.ok(conversations);
    }
}
