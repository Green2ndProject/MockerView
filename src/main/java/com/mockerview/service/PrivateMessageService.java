package com.mockerview.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.mockerview.dto.MessagePartnerResponse;
import com.mockerview.dto.PrivateMessageRequest;
import com.mockerview.dto.PrivateMessageResponse;
import com.mockerview.entity.PrivateMessage;
import com.mockerview.entity.PrivateMessageStatus;
import com.mockerview.repository.PrivateMessageRepository;
import com.mockerview.repository.PrivateMessageStatusRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrivateMessageService {

    private final SimpMessagingTemplate messagingTemplate;
    private final PrivateMessageRepository privateMessageRepository;
    private final PrivateMessageStatusRepository messageStatusRepository; 
    
    public void saveAndSend(String senderUsername, PrivateMessageRequest request){

        PrivateMessage message = PrivateMessage.builder()
                                    .senderUsername(senderUsername)
                                    .receiverUsername(request.getReceiverUsername())
                                    .content(request.getContent())
                                    .build();

        privateMessageRepository.save(message);

        PrivateMessageResponse response = PrivateMessageResponse.builder()
                                            .senderUsername(senderUsername)
                                            .receiverUsername(request.getReceiverUsername())
                                            .content(request.getContent())
                                            .sentAt(message.getSentAt())
                                            .build();

        messagingTemplate.convertAndSendToUser(
            request.getReceiverUsername(),
            "/queue/messages",
            response
        );

    }

    @Transactional(readOnly = true)
    public List<PrivateMessageResponse> getMessageHistory(String userA, String userB) {

        List<PrivateMessage> messages = 
            privateMessageRepository.findBySenderUsernameAndReceiverUsernameOrReceiverUsernameAndSenderUsernameOrderBySentAtAsc(
                userA, userB, // Sender = userA, Receiver = userB
                userA, userB  // Receiver = userA, Sender = userB
            );

        return messages.stream()
                .map(message -> PrivateMessageResponse.builder()
                                .senderUsername(message.getSenderUsername())
                                .receiverUsername(message.getReceiverUsername())
                                .content(message.getContent())
                                .sentAt(message.getSentAt())
                                .build())
                                .collect(Collectors.toList());                
                
        
                                
    }

    public List<MessagePartnerResponse> getMessagePartnerWithUnreadCount(String currentUsername){

        List<String> partnerUsernames = privateMessageRepository.findMyAllPartners(currentUsername);

        return partnerUsernames.stream()
                                .map(partnerUsername -> {
                                    PrivateMessageStatus status = messageStatusRepository
                                        .findByUserUsernameAndPartnerUsername(currentUsername, partnerUsername)
                                        .orElseGet(() -> PrivateMessageStatus.builder()
                                                .userUsername(currentUsername)
                                                .partnerUsername(partnerUsername)
                                                .lastReadMessageId(0L)
                                                .build());

                                long unreadCount = privateMessageRepository.countByReceiverUsernameAndIdGreaterThan(
                                    currentUsername,
                                    status.getLastReadMessageId()
                                );
                                
                                return MessagePartnerResponse.builder()
                                    .partnerUsername(partnerUsername)
                                    .unreadCount(unreadCount)
                                    .build();
                                })
                                .collect(Collectors.toList());
    }

    public void markMessageAsRead(String currentUsername, String partnerUsername){

        PrivateMessage lastMessage = privateMessageRepository.findTopBySenderUsernameAndReceiverUsernameOrReceiverUsernameAndSenderUsernameOrderByIdDesc(
            currentUsername, partnerUsername, currentUsername, partnerUsername
        ).orElse(null);

        if(lastMessage == null){

            return;
        }

        Long latestMessageId = lastMessage.getId();

        PrivateMessageStatus status = 
            messageStatusRepository.findByUserUsernameAndPartnerUsername(currentUsername, partnerUsername)
                .orElseGet(() -> PrivateMessageStatus.builder()
                                    .userUsername(currentUsername)
                                    .partnerUsername(partnerUsername)
                                    .build());

        if(status.getLastReadMessageId() >= latestMessageId){
            return;
        }          
        
        status.updateLastReadMessageId(latestMessageId);
        messageStatusRepository.save(status);

        messagingTemplate.convertAndSendToUser(
            partnerUsername, 
            "/queue/messagelist-update", 
            currentUsername);

    }
    

}
