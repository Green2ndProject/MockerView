package com.mockerview.controller.websocket;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.mockerview.dto.PrivateMessage;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class PrivateMessageController {
    
    private final SimpMessagingTemplate messagingTemplate;

    public PrivateMessageController(SimpMessagingTemplate messagingTemplate){
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/private.send")
    public void handlePrivateMessage(
        PrivateMessage message, 
        Principal principal
    ){
        try{
            String username = principal.getName();

            log.info("메세지 수신 : 보낸사람 : {}, 받는사람 : {}, 내용 : {}"
                    , message.getSenderId(), message.getReceiverId(), message.getContent());

            // messagingTemplate.convertAndSendToUser(
            //     message.getReceiverId(), "/queue/privatemessages", message);
        }catch(Exception e){
            log.error("❌ 메세지 전송 실패", e);
        }       
    }
}
