package com.mockerview.controller.websocket;

import com.mockerview.dto.AnswerMessage;
import com.mockerview.dto.InterviewerFeedbackMessage;
import com.mockerview.dto.SessionParticipantDTO;
import com.mockerview.dto.SessionStatusMessage;
import com.mockerview.entity.Answer;
import com.mockerview.entity.Session;
import com.mockerview.entity.SessionParticipant;
import com.mockerview.entity.User;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.QuestionRepository;
import com.mockerview.repository.SessionParticipantRepository;
import com.mockerview.repository.SessionRepository;
import com.mockerview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SessionWebSocketController {

        private final SimpMessagingTemplate messagingTemplate;
        private final SessionRepository sessionRepository;
        private final QuestionRepository questionRepository;
        private final AnswerRepository answerRepository;
        private final SessionParticipantRepository participantRepository;
        private final UserRepository userRepository;
        private final SessionEndHandler sessionEndHandler;

        private final Map<Long, Set<String>> sessionParticipants = new ConcurrentHashMap<>();

        @MessageMapping("/session/{sessionId}/join")
        public void handleJoin(
                @DestinationVariable Long sessionId,
                @Payload Map<String, Object> message
        ) {
                Long userId = Long.valueOf(message.get("userId").toString());
                String role = message.get("role").toString();
                
                log.info("🚪 WebSocket 참가 요청: sessionId={}, userId={}, role={}", sessionId, userId, role);

                Session session = sessionRepository.findById(sessionId)
                        .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다"));
                
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

                User.UserRole userRole = User.UserRole.valueOf(role);

                SessionParticipant participant = participantRepository
                        .findBySessionIdAndUserId(sessionId, userId)
                        .orElse(SessionParticipant.builder()
                                .session(session)
                                .user(user)
                                .role(userRole)
                                .build());
                
                participant.setRole(userRole);
                participant.setIsOnline(true);
                participant.setJoinedAt(LocalDateTime.now());
                participantRepository.save(participant);

                log.info("✅ WebSocket 참가자 저장: userId={}, role={}", userId, userRole);

                List<SessionParticipantDTO> participants = participantRepository
                        .findOnlineParticipants(sessionId)
                        .stream()
                        .map(SessionParticipantDTO::from)
                        .collect(Collectors.toList());

                log.info("📤 참가자 브로드캐스트: {}명", participants.size());
                participants.forEach(p -> log.info("   - userId={}, role={}, name={}", 
                        p.getUserId(), p.getRole(), p.getUserName()));

                messagingTemplate.convertAndSend("/topic/session/" + sessionId, participants);

                log.info("✅ WebSocket 참가 완료: userId={}, role={}", userId, userRole);
        }

        @MessageMapping("/session/{sessionId}/leave")
        public void handleLeave(
                @DestinationVariable Long sessionId,
                @Payload Map<String, Object> message
        ) {
                Long userId = Long.valueOf(message.get("userId").toString());
                log.info("퇴장 요청: sessionId={}, userId={}", sessionId, userId);

                participantRepository.findBySessionIdAndUserId(sessionId, userId)
                        .ifPresent(participant -> {
                                participant.setIsOnline(false);
                                participantRepository.save(participant);
                        });

                List<SessionParticipantDTO> participants = participantRepository
                        .findOnlineParticipants(sessionId)
                        .stream()
                        .map(SessionParticipantDTO::from)
                        .collect(Collectors.toList());

                messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/participants", participants);
                log.info("퇴장 완료: userId={}", userId);
        }

        @MessageMapping("/session/{sessionId}/question")
        public void handleQuestion(
                @DestinationVariable Long sessionId,
                @Payload Map<String, Object> message,
                SimpMessageHeaderAccessor headerAccessor
        ) {
                log.info("질문 메시지: sessionId={}, message={}", sessionId, message);
                
                try {
                        String content = (String) message.get("content");
                        Long userId = Long.valueOf(message.get("userId").toString());
                        
                        Session session = sessionRepository.findById(sessionId)
                                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다"));
                        
                        User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
                        
                        long questionCount = questionRepository.countBySessionId(sessionId);
                        
                        com.mockerview.entity.Question question = com.mockerview.entity.Question.builder()
                                .session(session)
                                .questioner(user)
                                .text(content)
                                .orderNo((int) questionCount + 1)
                                .createdAt(java.time.LocalDateTime.now())
                                .build();
                        
                        questionRepository.save(question);
                        log.info("✅ 질문 DB 저장 완료: questionId={}, text={}", question.getId(), content);
                        
                } catch (Exception e) {
                        log.error("❌ 질문 DB 저장 실패", e);
                }
                
                messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/question", message);
        }

        @MessageMapping("/session/{sessionId}/answer")
        public void handleAnswer(
                @DestinationVariable Long sessionId,
                @Payload AnswerMessage message,
                SimpMessageHeaderAccessor headerAccessor
        ) {
                log.info("답변 메시지: sessionId={}, message={}", sessionId, message);
                
                Long answerId = null;
                try {
                        User user = userRepository.findById(message.getUserId())
                                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
                        
                        List<com.mockerview.entity.Question> questions = questionRepository.findBySessionIdOrderByOrderNo(sessionId);
                        
                        if (!questions.isEmpty()) {
                                com.mockerview.entity.Question lastQuestion = questions.get(questions.size() - 1);
                                
                                Answer answer = Answer.builder()
                                        .question(lastQuestion)
                                        .user(user)
                                        .answerText(message.getAnswerText())
                                        .createdAt(java.time.LocalDateTime.now())
                                        .build();
                                
                                answerRepository.save(answer);
                                answerId = answer.getId();
                                log.info("✅ 답변 DB 저장 완료: answerId={}, questionId={}, text={}", 
                                        answer.getId(), lastQuestion.getId(), message.getAnswerText());
                        } else {
                                log.warn("⚠️ 질문이 없어서 답변 저장 실패");
                        }
                        
                } catch (Exception e) {
                        log.error("❌ 답변 DB 저장 실패", e);
                }
                
                if (answerId != null) {
                        message.setAnswerId(answerId);
                }
                
                messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/answer", message);
        }

        @MessageMapping("/session/{sessionId}/interviewer-feedback")
        public void handleInterviewerFeedback(
                @DestinationVariable Long sessionId,
                @Payload InterviewerFeedbackMessage message
        ) {
                log.info("면접관 피드백: sessionId={}, message={}", sessionId, message);
                messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/interviewer-feedback", message);
        }

        @MessageMapping("/session/{sessionId}/start")
        public void handleStartSession(@DestinationVariable Long sessionId) {
                log.info("세션 시작: sessionId={}", sessionId);
                messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/start", 
                Map.of("action", "START"));
        }

        @MessageMapping("/session/{sessionId}/end")
        public void handleEndSession(@DestinationVariable Long sessionId) {
                try {
                Session session = sessionRepository.findById(sessionId)
                        .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다"));

                session.setStatus(Session.SessionStatus.ENDED);
                sessionRepository.save(session);

                messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/control", 
                        Map.of("action", "END", "message", "면접이 종료되었습니다"));

                sessionParticipants.remove(sessionId);

                log.info("✅ 세션 종료: {}", sessionId);

                sessionEndHandler.handleSessionEnd(sessionId);

                } catch (Exception e) {
                log.error("❌ 세션 종료 실패: {}", e.getMessage());
                }
        }

        @MessageMapping("/session/{sessionId}/control")
        public void handleControl(
                @DestinationVariable Long sessionId,
                @Payload Map<String, Object> message,
                SimpMessageHeaderAccessor headerAccessor
        ) {
                log.info("제어 메시지: sessionId={}, message={}", sessionId, message);
                messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/control", message);
        }

        @MessageMapping("/session/{sessionId}/subtitle")
        public void handleSubtitle(
                @DestinationVariable Long sessionId,
                @Payload Map<String, Object> message
        ) {
                try {
                        Long userId = Long.valueOf(message.get("userId").toString());
                        String text = (String) message.get("text");
                        Boolean isFinal = (Boolean) message.get("isFinal");
                        
                        log.info("자막 메시지: sessionId={}, userId={}, text={}, isFinal={}", 
                                sessionId, userId, text, isFinal);

                        if (Boolean.TRUE.equals(isFinal) && text != null && !text.trim().isEmpty()) {
                                Session session = sessionRepository.findById(sessionId)
                                        .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다"));
                                
                                User user = userRepository.findById(userId)
                                        .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

                                SessionParticipant participant = participantRepository
                                        .findBySessionIdAndUserId(sessionId, userId)
                                        .orElseThrow(() -> new RuntimeException("참가자 정보를 찾을 수 없습니다"));

                                if (participant.getRole() == User.UserRole.HOST || 
                                        participant.getRole() == User.UserRole.REVIEWER) {
                                        long questionCount = questionRepository.countBySessionId(sessionId);
                                        
                                        com.mockerview.entity.Question question = com.mockerview.entity.Question.builder()
                                                .session(session)
                                                .questioner(user)
                                                .text(text.trim())
                                                .orderNo((int) questionCount + 1)
                                                .createdAt(LocalDateTime.now())
                                                .build();
                                        
                                        questionRepository.save(question);
                                        log.info("✅ 자막 → 질문 저장 완료: questionId={}, text={}", question.getId(), text.trim());
                                        
                                } else if (participant.getRole() == User.UserRole.STUDENT) {
                                        List<com.mockerview.entity.Question> questions = 
                                                questionRepository.findBySessionIdOrderByOrderNo(sessionId);
                                        
                                        if (!questions.isEmpty()) {
                                                com.mockerview.entity.Question lastQuestion = questions.get(questions.size() - 1);
                                                
                                                Answer answer = Answer.builder()
                                                        .question(lastQuestion)
                                                        .user(user)
                                                        .answerText(text.trim())
                                                        .createdAt(LocalDateTime.now())
                                                        .build();
                                                
                                                answerRepository.save(answer);
                                                message.put("answerId", answer.getId());
                                                log.info("✅ 자막 → 답변 저장 완료: answerId={}, questionId={}, text={}", 
                                                        answer.getId(), lastQuestion.getId(), text.trim());
                                        } else {
                                                log.warn("⚠️ 질문이 없어서 답변 저장 실패");
                                        }
                                }
                        }
                        
                } catch (Exception e) {
                        log.error("❌ 자막 처리 실패", e);
                }
                
                messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/subtitle", message);
        }
}
