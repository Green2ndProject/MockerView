package com.mockerview.service;

import com.mockerview.dto.SelfInterviewCreateDTO;
import com.mockerview.entity.*;
import com.mockerview.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SelfInterviewService {

    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final AIFeedbackService aiFeedbackService;

    @Transactional
    public Session createSelfInterview(Long userId, SelfInterviewCreateDTO dto) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Session session = Session.builder()
            .host(user)
            .title(dto.getTitle())
            .sessionType("SELF")
            .status(Session.SessionStatus.RUNNING)
            .startTime(LocalDateTime.now())
            .isReviewable("Y")
            .questions(new ArrayList<>())
            .build();

        session = sessionRepository.save(session);
        log.info("Created self interview session: {}", session.getId());

        List<String> aiQuestions = aiFeedbackService.generateInterviewQuestions(dto.getQuestionCount());
        log.info("Generated {} AI questions", aiQuestions.size());

        for (int i = 0; i < aiQuestions.size(); i++) {
            Question question = Question.builder()
                .session(session)
                .text(aiQuestions.get(i))
                .orderNo(i + 1)
                .questioner(user)
                .build();
            
            session.getQuestions().add(question);
            questionRepository.save(question);
        }

        return session;
    }

    @Transactional(readOnly = true)
    public List<Session> getUserSelfInterviews(Long userId) {
        return sessionRepository.findByHostIdAndSessionTypeOrderByCreatedAtDesc(userId, "SELF");
    }

    @Transactional(readOnly = true)
    public Session getSessionById(Long sessionId) {
        return sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다."));
    }

    @Transactional
    public Answer submitAnswer(Long sessionId, Long questionId, Long userId, String answerText) {
        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new RuntimeException("질문을 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Answer answer = Answer.builder()
            .question(question)
            .user(user)
            .answerText(answerText)
            .build();

        return answerRepository.save(answer);
    }

    @Transactional
    public Feedback generateAIFeedback(Answer answer) {
        Map<String, Object> aiResult = aiFeedbackService.generateFeedbackSync(
            answer.getQuestion().getText(), 
            answer.getAnswerText()
        );

        Feedback feedback = Feedback.builder()
            .answer(answer)
            .feedbackType(Feedback.FeedbackType.AI)
            .score(((Number) aiResult.get("score")).intValue())
            .strengths((String) aiResult.get("strengths"))
            .improvement((String) aiResult.get("improvements"))
            .model("GPT-4O-MINI")
            .build();

        return feedbackRepository.save(feedback);
    }
}
