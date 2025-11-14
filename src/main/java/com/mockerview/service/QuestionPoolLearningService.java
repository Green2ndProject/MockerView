package com.mockerview.service;

import com.mockerview.entity.Answer;
import com.mockerview.entity.Feedback;
import com.mockerview.entity.Question;
import com.mockerview.entity.QuestionPool;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.FeedbackRepository;
import com.mockerview.repository.QuestionPoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionPoolLearningService {

    private final AnswerRepository answerRepository;
    private final FeedbackRepository feedbackRepository;
    private final QuestionPoolRepository questionPoolRepository;

    private static final int MIN_ANSWERS_FOR_LEARNING = 5;
    private static final double MIN_AVG_SCORE_FOR_POOL = 70.0;

    @Transactional
    public void analyzeAndLearn() {
        log.info("🧠 QuestionPool 학습 시작...");
        
        List<Question> candidateQuestions = answerRepository.findQuestionsWithMinAnswers(MIN_ANSWERS_FOR_LEARNING);
        
        log.info("📊 분석 대상 질문: {}개", candidateQuestions.size());
        
        int savedCount = 0;
        
        for (Question question : candidateQuestions) {
            List<Answer> answers = answerRepository.findByQuestionId(question.getId());
            
            if (answers.isEmpty()) continue;
            
            List<Feedback> feedbacks = feedbackRepository.findByAnswerIn(answers);
            
            if (feedbacks.isEmpty()) continue;
            
            double avgScore = feedbacks.stream()
                .filter(f -> f.getScore() != null)
                .mapToDouble(Feedback::getScore)
                .average()
                .orElse(0.0);
            
            if (avgScore >= MIN_AVG_SCORE_FOR_POOL) {
                String category = question.getCategory() != null ? 
                    question.getCategory().getCode() : "GENERAL";
                String questionText = question.getText();
                
                boolean alreadyExists = questionPoolRepository.existsByTextAndCategory(questionText, category);
                
                if (!alreadyExists) {
                    saveToQuestionPool(question, avgScore);
                    savedCount++;
                    
                    log.info("✅ 저장 완료 - 질문: {}, 평균 점수: {:.2f}", 
                        questionText.substring(0, Math.min(50, questionText.length())), avgScore);
                }
            }
        }
        
        log.info("🎉 QuestionPool 학습 완료 - 총 {}개 질문 추가", savedCount);
    }

    private void saveToQuestionPool(Question question, double avgScore) {
        QuestionPool poolQuestion = QuestionPool.builder()
            .text(question.getText())
            .category(question.getCategory() != null ? question.getCategory().getCode() : "GENERAL")
            .difficulty(String.valueOf(question.getDifficultyLevel()))
            .build();

        questionPoolRepository.save(poolQuestion);

        log.info("💾 QuestionPool 저장 완료 - 평균 점수: {}, 카테고리: {}, 난이도: {}",
            avgScore, poolQuestion.getCategory(), poolQuestion.getDifficulty());
    }

    public long getQuestionPoolSize() {
        return questionPoolRepository.count();
    }

    public double getAiUsageRate() {
        long totalQuestions = answerRepository.countDistinctQuestions();
        long aiGeneratedQuestions = answerRepository.countAiGeneratedQuestions();

        if (totalQuestions == 0) return 100.0;

        return (aiGeneratedQuestions * 100.0) / totalQuestions;
    }
}
