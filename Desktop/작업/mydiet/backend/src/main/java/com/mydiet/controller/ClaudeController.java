package com.mydiet.controller;

import com.mydiet.service.ClaudeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/claude")
public class ClaudeController {

    @Autowired
    private ClaudeService claudeService;

    @GetMapping("/message")
    public ResponseEntity<String> getClaudeMessage(@RequestParam Long userId) {
        String message = claudeService.generateResponse(userId);
        return ResponseEntity.ok(message);
    }
}
