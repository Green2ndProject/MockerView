package com.mockerview.service;

import com.mockerview.dto.SelfInterviewQuestion;
import com.mockerview.dto.SelfInterviewSession;
import com.mockerview.entity.Question;
import com.mockerview.entity.Session;
import com.mockerview.entity.User;
import com.mockerview.entity.Answer;
import com.mockerview.entity.Feedback;
import com.mockerview.repository.QuestionRepository;
import com.mockerview.repository.SessionRepository;
import com.mockerview.repository.UserRepository;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public Session createSelfInterviewSession(User user, String title, Integer questionCount) {
        if (questionCount == null || questionCount < 1) {
            questionCount = 5;
        }

        Session session = Session.builder()
            .title(title != null ? title : "셀프 면접 연습 - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
            .host(user)
            .status(Session.SessionStatus.RUNNING)
            .isReviewable("N")
            .build();
        session = sessionRepository.save(session);

        List<String> aiQuestions = aiFeedbackService.generateInterviewQuestions(questionCount);
        
        log.info("AI가 생성한 질문 수: {}", aiQuestions.size());
        aiQuestions.forEach(q -> log.info("생성된 질문: {}", q));

        for (int i = 0; i < aiQuestions.size(); i++) {
            Question question = Question.builder()
                .session(session)
                .questioner(user)
                .text(aiQuestions.get(i))
                .orderNo(i + 1)
                .timer(120)
                .build();
            questionRepository.save(question);
        }

        return session;
    }

    @Transactional
    public SelfInterviewSession createSession(Long userId, int questionCount) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Session session = Session.builder()
            .title("셀프 면접 연습 - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
            .host(user)
            .status(Session.SessionStatus.RUNNING)
            .isReviewable("N")
            .build();
        session = sessionRepository.save(session);

        List<String> aiQuestions = aiFeedbackService.generateInterviewQuestions(questionCount);
        
        log.info("AI가 생성한 질문 수: {}", aiQuestions.size());
        aiQuestions.forEach(q -> log.info("생성된 질문: {}", q));

        for (int i = 0; i < aiQuestions.size(); i++) {
            Question question = Question.builder()
                .session(session)
                .questioner(user)
                .text(aiQuestions.get(i))
                .orderNo(i + 1)
                .timer(120)
                .build();
            questionRepository.save(question);
        }

        List<SelfInterviewQuestion> questions = questionRepository.findBySessionIdOrderByOrderNo(session.getId())
            .stream()
            .map(q -> SelfInterviewQuestion.builder()
                .id(q.getId())
                .questionText(q.getText())
                .orderNo(q.getOrderNo())
                .build())
            .collect(Collectors.toList());

        return SelfInterviewSession.builder()
            .sessionId(session.getId())
            .questions(questions)
            .build();
    }

    @Transactional(readOnly = true)
    public SelfInterviewSession getSession(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        List<SelfInterviewQuestion> questions = questionRepository.findBySessionIdOrderByOrderNo(sessionId)
            .stream()
            .map(q -> SelfInterviewQuestion.builder()
                .id(q.getId())
                .questionText(q.getText())
                .orderNo(q.getOrderNo())
                .build())
            .collect(Collectors.toList());

        return SelfInterviewSession.builder()
            .sessionId(sessionId)
            .questions(questions)
            .build();
    }

    @Transactional
    public Map<String, Object> submitAnswer(Long sessionId, Long questionId, String answerText, Long userId) {
        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new RuntimeException("Question not found"));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Answer answer = Answer.builder()
            .question(question)
            .user(user)
            .answerText(answerText)
            .build();
        answer = answerRepository.save(answer);

        Map<String, Object> feedback = aiFeedbackService.generateFeedbackSync(question.getText(), answerText);

        Feedback feedbackEntity = Feedback.builder()
            .answer(answer)
            .summary("AI 평가 완료")
            .strengths((String) feedback.get("strengths"))
            .weaknesses("")
            .improvement((String) feedback.get("improvements"))
            .score((Integer) feedback.get("score"))
            .model("GPT-4O-MINI")
            .build();
        feedbackRepository.save(feedbackEntity);

        return Map.of(
            "answer", Map.of(
                "id", answer.getId(),
                "answerText", answer.getAnswerText(),
                "questionId", questionId
            ),
            "feedback", feedback
        );
    }
}