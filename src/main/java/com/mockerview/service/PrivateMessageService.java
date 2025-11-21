package com.mockerview.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.mockerview.dto.ConversationSummaryDTO;
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

        String receiverUsername = request.getReceiverUsername();

        PrivateMessage message = PrivateMessage.builder()
                                    .senderUsername(senderUsername)
                                    .receiverUsername(request.getReceiverUsername())
                                    .content(request.getContent())
                                    .build();

        privateMessageRepository.save(message);

        PrivateMessageResponse response = PrivateMessageResponse.builder()
                                            .senderUsername(senderUsername)
                                            .receiverUsername(receiverUsername)
                                            .content(request.getContent())
                                            .sentAt(message.getSentAt())
                                            .build();

        messagingTemplate.convertAndSendToUser(
            receiverUsername,
            "/queue/messages",
            response
        );

        List<ConversationSummaryDTO> receiverSummaries = getConversationSummaries(receiverUsername);

        messagingTemplate.convertAndSendToUser(
            receiverUsername, 
            "/queue/messagelist-update",
            receiverSummaries
        );

        List<ConversationSummaryDTO> senderSummaries = getConversationSummaries(senderUsername);

        messagingTemplate.convertAndSendToUser(
            senderUsername,
            "/queue/messagelist-update",
            senderSummaries
        );

        // 토스트 알림 푸시
        messagingTemplate.convertAndSendToUser(
            receiverUsername, 
            "/queue/notification", 
            Map.of(
                "senderUsername", senderUsername,
                "messageSnippet", request.getContent().substring(0, Math.min(request.getContent().length(), 50)) // 미리보기 내용
            )
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

    // public List<MessagePartnerResponse> getMessagePartnerWithUnreadCount(String currentUsername){

    //     List<String> partnerUsernames = privateMessageRepository.findMyAllPartners(currentUsername);

    //     return partnerUsernames.stream()
    //                             .map(partnerUsername -> {
    //                                 PrivateMessageStatus status = messageStatusRepository
    //                                     .findByUserUsernameAndPartnerUsername(currentUsername, partnerUsername)
    //                                     .orElseGet(() -> PrivateMessageStatus.builder()
    //                                             .userUsername(currentUsername)
    //                                             .partnerUsername(partnerUsername)
    //                                             .lastReadMessageId(0L)
    //                                             .build());

    //                             long unreadCount = privateMessageRepository.countByReceiverUsernameAndIdGreaterThan(
    //                                 currentUsername,
    //                                 status.getLastReadMessageId()
    //                             );
                                
    //                             return MessagePartnerResponse.builder()
    //                                 .partnerUsername(partnerUsername)
    //                                 .unreadCount(unreadCount)
    //                                 .build();
    //                             })
    //                             .collect(Collectors.toList());
    // }

    @Transactional
    public void markMessageAsRead(String currentUsername, String partnerUsername){

        System.out.println("DEBUG: markMessageAsRead 메서드 실행 시작!");
        System.out.println("DEBUG: currentUsername (u1): " + currentUsername);
        System.out.println("DEBUG: partnerUsername (u2): " + partnerUsername);

        PrivateMessage lastMessage = 
            privateMessageRepository.findTopLatestMessage(
                currentUsername, partnerUsername
            ).orElse(null);

        if(lastMessage == null){
            System.out.println("DEBUG: 메시지 기록 없음. 저장 건너뜀.");
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
            System.out.println("DEBUG: 이미 최신 메시지 ID로 읽음 처리됨. 저장 건너뜀.");
            return;
        }          
        
        status.updateLastReadMessageId(latestMessageId);
        messageStatusRepository.save(status);

        List<ConversationSummaryDTO> summaries = getConversationSummaries(currentUsername);

        messagingTemplate.convertAndSendToUser(
            partnerUsername, 
            "/queue/messagelist-update", 
            summaries);

        messagingTemplate.convertAndSendToUser(
            currentUsername,
            "/queue/messagelist-update",
            summaries);

    }

    public List<ConversationSummaryDTO> getConversationSummaries(String currentUsername) {
        // 대화 상대방 목록 조회
        List<String> partnerUsernames = privateMessageRepository.findDistinctPartners(currentUsername);

        List<ConversationSummaryDTO> summaries = new ArrayList<>();

        for(String partnerUsername : partnerUsernames){
            
            ConversationSummaryDTO summary = getSummaryForPartner(currentUsername, partnerUsername);

            summaries.add(summary);

        }

        summaries.sort(Comparator.comparing(ConversationSummaryDTO::getLastMessageSentAt).reversed());

        return summaries;
    }

    private ConversationSummaryDTO getSummaryForPartner(String currentUsername, String partnerUsername){

        // 대화상대와 마지막 메시지 조회
        PrivateMessage lastMessage = 
            privateMessageRepository.findFirstBySenderUsernameAndReceiverUsernameOrReceiverUsernameAndSenderUsernameOrderBySentAtDesc(
                currentUsername, partnerUsername, currentUsername, partnerUsername
            ).orElse(null);

        long unreadCount = calculateUnreadCount(currentUsername, partnerUsername);

        return new ConversationSummaryDTO(
            partnerUsername,
            lastMessage != null ? lastMessage.getContent() : "",
            lastMessage != null ? lastMessage.getSentAt() : null,
            unreadCount
        );
    }

    private long calculateUnreadCount(String currentUsername, String partnerUsername) {

        // 마지막으로 읽은 메세지 ID 를 들고와서
        PrivateMessageStatus status = 
            messageStatusRepository.findByUserUsernameAndPartnerUsername(
                currentUsername, partnerUsername
            ).orElseGet(() -> PrivateMessageStatus.builder()
                                                .userUsername(currentUsername)
                                                .partnerUsername(partnerUsername)
                                                .lastReadMessageId(0L)
                                                .build());
        // Long lastReadMessageId 넣고
        Long lastReadMessageId = status.getLastReadMessageId();
        // 카운트 쿼리 countUnreadMessages
        return privateMessageRepository.countUnreadMessages(
            currentUsername, partnerUsername, lastReadMessageId
        );
    }
    

}
