package com.mockerview.service;

import com.mockerview.dto.CategoryStatsDTO;
import com.mockerview.entity.*;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MyPageStatsService {

    private final AnswerRepository answerRepository;
    private final CategoryRepository categoryRepository;

    public Map<String, Object> getUserStats(User user) {
        List<Answer> allAnswers = answerRepository.findByUserIdWithFeedbacks(user.getId());

        long totalSessions = allAnswers.stream()
                .map(answer -> answer.getQuestion().getSession())
                .filter(Objects::nonNull)
                .distinct()
                .count();

        long totalAnswers = allAnswers.size();

        double avgScore = allAnswers.stream()
                .flatMap(answer -> answer.getFeedbacks().stream())
                .filter(feedback -> feedback.getScore() != null)
                .mapToInt(Feedback::getScore)
                .average()
                .orElse(0.0);

        long totalFeedbacks = allAnswers.stream()
                .flatMap(answer -> answer.getFeedbacks().stream())
                .count();

        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        long recentAnswers = allAnswers.stream()
                .filter(answer -> answer.getCreatedAt().isAfter(oneMonthAgo))
                .count();
        long previousAnswers = totalAnswers - recentAnswers;
        double growthRate = previousAnswers > 0 
            ? ((double) recentAnswers / previousAnswers - 1) * 100 
            : 0.0;

        Map<String, Integer> categoryCount = new HashMap<>();
        for (Answer answer : allAnswers) {
            Question question = answer.getQuestion();
            if (question != null) {
                Session session = question.getSession();
                if (session != null && session.getCategory() != null) {
                    String category = session.getCategory();
                    categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);
                }
            }
        }

        List<CategoryStatsDTO> categoryStats = categoryCount.entrySet().stream()
                .map(entry -> {
                    String categoryName = getCategoryName(entry.getKey());
                    return CategoryStatsDTO.builder()
                            .categoryCode(entry.getKey())
                            .categoryName(categoryName)
                            .count(entry.getValue())
                            .build();
                })
                .sorted((a, b) -> b.getCount().compareTo(a.getCount()))
                .limit(5)
                .collect(Collectors.toList());

        Map<String, Double> recentScores = new LinkedHashMap<>();
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        for (int i = 0; i < 7; i++) {
            LocalDateTime date = startDate.plusDays(i * 5);
            String dateStr = date.toLocalDate().toString();
            
            double dayScore = allAnswers.stream()
                    .filter(answer -> answer.getCreatedAt().isAfter(date) 
                                   && answer.getCreatedAt().isBefore(date.plusDays(5)))
                    .flatMap(answer -> answer.getFeedbacks().stream())
                    .filter(feedback -> feedback.getScore() != null)
                    .mapToInt(Feedback::getScore)
                    .average()
                    .orElse(0.0);
            
            recentScores.put(dateStr, dayScore);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalSessions", totalSessions);
        result.put("totalAnswers", totalAnswers);
        result.put("avgScore", Math.round(avgScore * 10) / 10.0);
        result.put("totalFeedbacks", totalFeedbacks);
        result.put("growthRate", Math.round(growthRate * 10) / 10.0);
        result.put("categoryStats", categoryStats);
        result.put("recentScores", recentScores);

        return result;
    }

    private String getCategoryName(String categoryCode) {
        return categoryRepository.findByCode(categoryCode)
                .map(Category::getName)
                .orElse(categoryCode);
    }
}
