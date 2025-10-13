package com.mockerview.controller.websocket;

import com.mockerview.dto.websocket.*;
import com.mockerview.entity.*;
import com.mockerview.repository.*;
import com.mockerview.service.AIFeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final AIFeedbackService aiFeedbackService;
    
    private final Map<Long, Set<String>> sessionParticipants = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, SessionParticipantInfo>> participantInfoMap = new ConcurrentHashMap<>();

    @MessageMapping("/session/{sessionId}/join")
    public void handleJoin(@DestinationVariable Long sessionId, SessionMessage message) {
        log.info("세션 참가: sessionId={}, userId={}, userName={}", sessionId, message.getUserId(), message.getUserName());
        
        sessionParticipants.putIfAbsent(sessionId, ConcurrentHashMap.newKeySet());
        sessionParticipants.get(sessionId).add(message.getUserName());
        
        participantInfoMap.putIfAbsent(sessionId, new ConcurrentHashMap<>());
        SessionParticipantInfo info = new SessionParticipantInfo();
        info.setUserId(message.getUserId());
        info.setUserName(message.getUserName());
        info.setJoinedAt(LocalDateTime.now());
        participantInfoMap.get(sessionId).put(String.valueOf(message.getUserId()), info);
        
        Map<String, Object> userMapping = new HashMap<>();
        userMapping.put("userId", message.getUserId());
        userMapping.put("userName", message.getUserName());
        userMapping.put("timestamp", LocalDateTime.now());
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/user-mapping", userMapping);
        
        Map<String, Object> statusUpdate = new HashMap<>();
        statusUpdate.put("action", "JOIN");
        statusUpdate.put("userName", message.getUserName());
        statusUpdate.put("participants", new ArrayList<>(sessionParticipants.get(sessionId)));
        statusUpdate.put("timestamp", LocalDateTime.now());
        
        long questionCount = questionRepository.countBySessionId(sessionId);
        long answerCount = answerRepository.countByQuestionSessionId(sessionId);
        statusUpdate.put("questionCount", questionCount);
        statusUpdate.put("answerCount", answerCount);
        
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/status", statusUpdate);
    }

    @MessageMapping("/session/{sessionId}/leave")
    public void handleLeave(@DestinationVariable Long sessionId, SessionMessage message) {
        log.info("세션 퇴장: sessionId={}, userName={}", sessionId, message.getUserName());
        
        if (sessionParticipants.containsKey(sessionId)) {
            sessionParticipants.get(sessionId).remove(message.getUserName());
        }
        
        if (participantInfoMap.containsKey(sessionId)) {
            participantInfoMap.get(sessionId).remove(String.valueOf(message.getUserId()));
        }
        
        Map<String, Object> statusUpdate = new HashMap<>();
        statusUpdate.put("action", "LEAVE");
        statusUpdate.put("userName", message.getUserName());
        statusUpdate.put("participants", sessionParticipants.containsKey(sessionId) ? 
            new ArrayList<>(sessionParticipants.get(sessionId)) : new ArrayList<>());
        statusUpdate.put("timestamp", LocalDateTime.now());
        
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/status", statusUpdate);
    }

    @MessageMapping("/session/{sessionId}/question")
    @Transactional
    public void handleQuestion(@DestinationVariable Long sessionId, QuestionMessage message) {
        log.info("질문 수신: sessionId={}, text={}", sessionId, message.getText());
        
        Session session = sessionRepository.findById(sessionId).orElseThrow();
        
        Question question = Question.builder()
            .session(session)
            .text(message.getText())
            .orderNo(message.getOrderNo())
            .timer(message.getTimerSeconds())
            .createdAt(LocalDateTime.now())
            .build();
        
        question = questionRepository.save(question);
        
        Map<String, Object> questionData = new HashMap<>();
        questionData.put("questionId", question.getId());
        questionData.put("questionText", question.getText());
        questionData.put("orderNo", question.getOrderNo());
        questionData.put("timer", question.getTimer());
        questionData.put("timestamp", LocalDateTime.now());
        
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/question", questionData);
    }

    @MessageMapping("/session/{sessionId}/answer")
    @Transactional
    public void handleAnswer(@DestinationVariable Long sessionId, AnswerMessage message) {
        log.info("답변 수신: sessionId={}, questionId={}, userId={}", sessionId, message.getQuestionId(), message.getUserId());
        
        Question question = questionRepository.findById(message.getQuestionId()).orElseThrow();
        User user = userRepository.findById(message.getUserId()).orElseThrow();
        
        Answer answer = Answer.builder()
            .question(question)
            .user(user)
            .answerText(message.getAnswerText())
            .createdAt(LocalDateTime.now())
            .build();
        
        answer = answerRepository.save(answer);
        
        Map<String, Object> answerData = new HashMap<>();
        answerData.put("answerId", answer.getId());
        answerData.put("questionId", question.getId());
        answerData.put("questionOrder", question.getOrderNo());
        answerData.put("questionText", question.getText());
        answerData.put("userId", user.getId());
        answerData.put("userName", user.getName());
        answerData.put("answerText", answer.getAnswerText());
        answerData.put("timestamp", LocalDateTime.now());
        
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/answer", answerData);
        
        try {
            Feedback aiFeedback = aiFeedbackService.generateFeedback(answer.getId(), question.getText(), answer.getAnswerText());
            
            Map<String, Object> feedbackData = new HashMap<>();
            feedbackData.put("answerId", answer.getId());
            feedbackData.put("questionOrder", question.getOrderNo());
            feedbackData.put("score", aiFeedback.getScore());
            feedbackData.put("strengths", aiFeedback.getStrengths());
            feedbackData.put("weaknesses", aiFeedback.getWeaknesses());
            feedbackData.put("improvementSuggestions", aiFeedback.getImprovementSuggestions());
            feedbackData.put("timestamp", LocalDateTime.now());
            
            messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/feedback", feedbackData);
        } catch (Exception e) {
            log.error("AI 피드백 생성 실패", e);
        }
    }

    @MessageMapping("/session/{sessionId}/interviewer-feedback")
    @Transactional
    public void handleInterviewerFeedback(@DestinationVariable Long sessionId, InterviewerFeedbackMessage message) {
        log.info("면접관 피드백: sessionId={}, answerId={}", sessionId, message.getAnswerId());
        
        Answer answer = answerRepository.findById(message.getAnswerId()).orElseThrow();
        User reviewer = userRepository.findById(message.getReviewerId()).orElseThrow();
        
        Feedback feedback = Feedback.builder()
            .answer(answer)
            .reviewer(reviewer)
            .feedbackType(Feedback.FeedbackType.INTERVIEWER)
            .score(message.getScore())
            .reviewerComment(message.getComment())
            .createdAt(LocalDateTime.now())
            .build();
        
        feedbackRepository.save(feedback);
        
        Map<String, Object> feedbackData = new HashMap<>();
        feedbackData.put("answerId", answer.getId());
        feedbackData.put("reviewerId", reviewer.getId());
        feedbackData.put("reviewerName", reviewer.getName());
        feedbackData.put("score", feedback.getScore());
        feedbackData.put("comment", feedback.getReviewerComment());
        feedbackData.put("timestamp", LocalDateTime.now());
        
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/interviewer-feedback", feedbackData);
    }

    @MessageMapping("/session/{sessionId}/control")
    public void handleControl(@DestinationVariable Long sessionId, Map<String, Object> message) {
        String action = (String) message.get("action");
        log.info("제어 메시지: sessionId={}, action={}", sessionId, action);
        
        Map<String, Object> controlData = new HashMap<>();
        controlData.put("action", action);
        controlData.put("timestamp", LocalDateTime.now());
        
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/control", controlData);
    }
}