package com.mockerview.controller.api;

import com.mockerview.dto.AgoraTokenDTO;
import com.mockerview.service.AgoraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.mockerview.dto.CustomUserDetails;

@RestController
@RequestMapping("/api/agora")
@RequiredArgsConstructor
public class AgoraApiController {

    private final AgoraService agoraService;

    @PostMapping("/token/{sessionId}")
    public ResponseEntity<AgoraTokenDTO> getToken(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Long userId = userDetails.getUserId();
        AgoraTokenDTO token = agoraService.generateTokenForSession(sessionId, userId);
        
        return ResponseEntity.ok(token);
    }
}
