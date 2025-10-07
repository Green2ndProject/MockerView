package com.mockerview.service;

import com.mockerview.entity.Question;
import com.mockerview.entity.QuestionPool;
import com.mockerview.entity.Session;
import com.mockerview.entity.User;
import com.mockerview.repository.QuestionPoolRepository;
import com.mockerview.repository.QuestionRepository;
import com.mockerview.repository.SessionRepository;
import com.mockerview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SelfInterviewService {

    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionPoolRepository questionPoolRepository;
    private final UserRepository userRepository;

    @Transactional
    public Session createSelfInterviewSession(User user, String title, Integer questionCount, String sessionType) {
        Session session = Session.builder()
            .host(user)
            .title(title)
            .sessionStatus(Session.SessionStatus.RUNNING)
            .sessionType(sessionType != null ? sessionType : "TEXT")
            .isSelfInterview("Y")
            .isReviewable("Y")
            .createdAt(LocalDateTime.now())
            .startTime(LocalDateTime.now())
            .lastActivity(LocalDateTime.now())
            .build();

        Session savedSession = sessionRepository.save(session);

        List<QuestionPool> randomQuestions = questionPoolRepository.findRandomQuestions(questionCount);
        
        int orderNo = 1;
        for (QuestionPool qp : randomQuestions) {
            Question question = Question.builder()
                .session(savedSession)
                .text(qp.getText())
                .orderNo(orderNo++)
                .questioner(user)
                .timer(120)
                .build();
            questionRepository.save(question);
        }

        log.info("셀프 면접 생성 완료 - sessionId: {}, type: {}, 질문 수: {}", 
            savedSession.getId(), sessionType, randomQuestions.size());
        return savedSession;
    }

    @Transactional
    public Session createSelfInterview(Long userId, String interviewType, Integer questionCount) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        return createSelfInterviewSession(user, "셀프 면접 - " + interviewType, questionCount, "TEXT");
    }

    @Transactional
    public Session createSelfInterviewWithAI(Long userId, String interviewType, Integer questionCount) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        return createSelfInterviewSession(user, "AI 셀프 면접 - " + interviewType, questionCount, "TEXT");
    }
}