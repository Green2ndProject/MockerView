package com.mockerview.controller.api;

import com.mockerview.dto.SessionParticipantDTO;
import com.mockerview.entity.Session;
import com.mockerview.entity.SessionParticipant;
import com.mockerview.entity.User;
import com.mockerview.repository.SessionParticipantRepository;
import com.mockerview.repository.SessionRepository;
import com.mockerview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/session/{sessionId}/participants")
@RequiredArgsConstructor
public class SessionParticipantApiController {

        private final SessionParticipantRepository participantRepository;
        private final SessionRepository sessionRepository;
        private final UserRepository userRepository;
        private final SimpMessagingTemplate messagingTemplate;

        @GetMapping
        public ResponseEntity<?> getParticipants(@PathVariable Long sessionId) {
                try {
                        log.info("📋 세션 참가자 목록 요청 - sessionId: {}", sessionId);
                        
                        List<SessionParticipant> rawParticipants = participantRepository.findOnlineParticipants(sessionId);
                        log.info("🔍 DB 조회 결과: {}명", rawParticipants.size());
                        
                        List<SessionParticipantDTO> participants = rawParticipants
                                .stream()
                                .map(p -> {
                                        try {
                                                SessionParticipantDTO dto = SessionParticipantDTO.from(p);
                                                log.info("  - DTO 생성: userId={}, userName={}, role={}", 
                                                        dto.getUserId(), dto.getUserName(), dto.getRole());
                                                return dto;
                                        } catch (Exception e) {
                                                log.error("  ❌ DTO 변환 실패: participant.id={}", p.getId(), e);
                                                return null;
                                        }
                                })
                                .filter(dto -> dto != null)
                                .collect(Collectors.toList());
                        
                        log.info("✅ 참가자 {}명 반환", participants.size());
                        return ResponseEntity.ok(participants);
                } catch (Exception e) {
                        log.error("❌ 참가자 목록 조회 실패 - sessionId: {}", sessionId, e);
                        return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
                }
        }

        @PostMapping("/join")
        public ResponseEntity<SessionParticipantDTO> joinSession(
                @PathVariable Long sessionId,
                @RequestParam String role
        ) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String username = authentication.getName();
                
                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
                
                Session session = sessionRepository.findById(sessionId)
                        .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다"));

                User.UserRole userRole = User.UserRole.valueOf(role);

                SessionParticipant participant = participantRepository
                        .findBySessionIdAndUserId(sessionId, user.getId())
                        .orElse(SessionParticipant.builder()
                                .session(session)
                                .user(user)
                                .role(userRole)
                                .build());
                
                participant.setRole(userRole);
                participant.setIsOnline(true);
                participant.setJoinedAt(LocalDateTime.now());
                participantRepository.save(participant);

                log.info("✅ 참가자 저장 완료: sessionId={}, userId={}, role={}", sessionId, user.getId(), userRole);

                List<SessionParticipantDTO> participants = participantRepository
                        .findOnlineParticipants(sessionId)
                        .stream()
                        .map(SessionParticipantDTO::from)
                        .collect(Collectors.toList());

                messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/participants", participants);
                
                log.info("📤 WebSocket 브로드캐스트: sessionId={}, 참가자 수: {}", sessionId, participants.size());
                participants.forEach(p -> log.info("  - userId={}, role={}, name={}", p.getUserId(), p.getRole(), p.getUserName()));

                return ResponseEntity.ok(SessionParticipantDTO.from(participant));
        }

        @PostMapping("/leave")
        public ResponseEntity<Void> leaveSession(@PathVariable Long sessionId) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String username = authentication.getName();
                
                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

                participantRepository.findBySessionIdAndUserId(sessionId, user.getId())
                        .ifPresent(participant -> {
                        participant.setIsOnline(false);
                        participant.setLeftAt(LocalDateTime.now());
                        participantRepository.save(participant);
                        });

                List<SessionParticipantDTO> participants = participantRepository
                        .findOnlineParticipants(sessionId)
                        .stream()
                        .map(SessionParticipantDTO::from)
                        .collect(Collectors.toList());

                messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/participants", participants);
                
                log.info("퇴장 완료: sessionId={}, userId={}, 현재 참가자 수: {}", 
                        sessionId, user.getId(), participants.size());

                return ResponseEntity.ok().build();
        }
}