package com.mockerview.service;

import com.mockerview.dto.InterviewerFeedbackMessage;
import com.mockerview.entity.Answer;
import com.mockerview.entity.Feedback;
import com.mockerview.entity.User;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.FeedbackRepository;
import com.mockerview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewerFeedbackService {

    private final AnswerRepository answerRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PushNotificationService pushNotificationService;

    @Transactional
    public void submitInterviewerFeedback(InterviewerFeedbackMessage feedbackMessage) {
        try {
            Answer answer = answerRepository.findById(feedbackMessage.getAnswerId())
                    .orElseThrow(() -> new RuntimeException("Answer not found: " + feedbackMessage.getAnswerId()));

            User reviewer = userRepository.findById(feedbackMessage.getReviewerId())
                    .orElseThrow(() -> new RuntimeException("Reviewer not found: " + feedbackMessage.getReviewerId()));

            Feedback feedback = Feedback.builder()
                    .answer(answer)
                    .reviewer(reviewer)
                    .score(feedbackMessage.getScore())
                    .reviewerComment(feedbackMessage.getComment())
                    .feedbackType(Feedback.FeedbackType.INTERVIEWER)
                    .summary(feedbackMessage.getComment())
                    .strengths(null)
                    .weaknesses(null)
                    .improvementSuggestions(null)
                    .build();

            feedbackRepository.save(feedback);

            feedbackMessage.setTimestamp(LocalDateTime.now());

            log.info("Interviewer feedback submitted successfully for answer: {}", feedbackMessage.getAnswerId());

            if (answer.getUser() != null) {
                pushNotificationService.sendNotification(
                        answer.getUser(),
                        "👨‍💼 면접관 피드백 도착!",
                        reviewer.getName() + "님이 피드백을 남겼습니다. 점수: " + feedbackMessage.getScore() + "점",
                        "/session/" + feedbackMessage.getSessionId());
            }

            log.info("Interviewer feedback submitted successfully");

        } catch (Exception e) {
            log.error("Error submitting interviewer feedback: ", e);
            throw new RuntimeException("면접관 피드백 제출 실패", e);
        }
    }
}