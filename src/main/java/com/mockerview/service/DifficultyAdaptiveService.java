package com.mockerview.service;

import com.mockerview.entity.*;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DifficultyAdaptiveService {

    private final AnswerRepository answerRepository;
    private final CategoryRepository categoryRepository;
    private final AIQuestionGeneratorService aiQuestionGenerator;
    
    private static final int SCORE_THRESHOLD_HIGH = 75;
    private static final int SCORE_THRESHOLD_LOW = 50;
    private static final int MIN_DIFFICULTY = 1;
    private static final int MAX_DIFFICULTY = 5;

    @Transactional
    public Question generateAdaptiveQuestion(Session session, User user, String categoryCode, String questionType) {
        
        Category category = categoryRepository.findByCode(categoryCode)
                .orElseGet(() -> categoryRepository.findByCode("IT_DEV").orElse(null));
        
        if (category == null) {
            log.error("카테고리를 찾을 수 없음: {}", categoryCode);
            throw new IllegalArgumentException("Invalid category: " + categoryCode);
        }

        Integer currentDifficulty = calculateNextDifficulty(session, user);
        
        String previousFeedback = getLatestFeedback(session, user);
        
        Question newQuestion = aiQuestionGenerator.generateQuestion(
                category, 
                currentDifficulty, 
                questionType, 
                session, 
                previousFeedback
        );

        log.info("🎯 적응형 질문 생성 - 사용자: {}, 난이도: {}, 질문: {}", 
                user.getUsername(), currentDifficulty, newQuestion.getText());

        return newQuestion;
    }

    private Integer calculateNextDifficulty(Session session, User user) {
        List<Answer> userAnswers = answerRepository.findBySessionAndAnswerer(session, user);
        
        if (userAnswers.isEmpty()) {
            return 2;
        }

        Answer lastAnswer = userAnswers.get(userAnswers.size() - 1);
        Integer currentDifficulty = Optional.ofNullable(lastAnswer.getQuestion().getDifficultyLevel()).orElse(2);
        
        Integer avgScore = calculateAverageScore(userAnswers);

        Integer nextDifficulty;
        if (avgScore >= SCORE_THRESHOLD_HIGH) {
            nextDifficulty = Math.min(currentDifficulty + 1, MAX_DIFFICULTY);
            log.info("⬆️ 난이도 상승: {} → {} (평균 점수: {})", currentDifficulty, nextDifficulty, avgScore);
        } else if (avgScore < SCORE_THRESHOLD_LOW && currentDifficulty > MIN_DIFFICULTY) {
            nextDifficulty = Math.max(currentDifficulty - 1, MIN_DIFFICULTY);
            log.info("⬇️ 난이도 하락: {} → {} (평균 점수: {})", currentDifficulty, nextDifficulty, avgScore);
        } else {
            nextDifficulty = currentDifficulty;
            log.info("➡️ 난이도 유지: {} (평균 점수: {})", nextDifficulty, avgScore);
        }

        return nextDifficulty;
    }

    private Integer calculateAverageScore(List<Answer> answers) {
        if (answers.isEmpty()) return 0;
        
        int recentCount = Math.min(3, answers.size());
        List<Answer> recentAnswers = answers.subList(answers.size() - recentCount, answers.size());
        
        return (int) recentAnswers.stream()
                .map(Answer::getScore)
                .filter(score -> score != null && score > 0)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(50.0);
    }

    private String getLatestFeedback(Session session, User user) {
        List<Answer> userAnswers = answerRepository.findBySessionAndAnswerer(session, user);
        
        if (userAnswers.isEmpty()) return null;
        
        Answer lastAnswer = userAnswers.get(userAnswers.size() - 1);
        
        if (lastAnswer.getFeedbacks() != null && !lastAnswer.getFeedbacks().isEmpty()) {
            Feedback lastFeedback = lastAnswer.getFeedbacks().get(lastAnswer.getFeedbacks().size() - 1);
            String comment = lastFeedback.getReviewerComment() != null ? 
                    lastFeedback.getReviewerComment() : lastFeedback.getSummary();
            return String.format("점수: %d, 코멘트: %s", 
                    lastAnswer.getScore(), 
                    comment);
        }
        
        return lastAnswer.getScore() != null ? "이전 점수: " + lastAnswer.getScore() : null;
    }

    public String getDifficultyDescription(Integer level) {
        return switch (level) {
            case 1 -> "초급 (기본 개념)";
            case 2 -> "초중급 (실무 기초)";
            case 3 -> "중급 (실무 경험)";
            case 4 -> "중고급 (전략적 사고)";
            case 5 -> "고급 (전문가 수준)";
            default -> "중급";
        };
    }
}
