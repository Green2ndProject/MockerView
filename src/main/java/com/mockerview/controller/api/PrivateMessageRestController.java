package com.mockerview.controller.api;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockerview.dto.PrivateMessageResponse;
import com.mockerview.service.PrivateMessageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


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

    @PutMapping("path/{id}")
    public String putMethodName(@PathVariable String id, @RequestBody String entity) {
        //TODO: process PUT request
        
        return entity;
    }
}
