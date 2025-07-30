package com.mydiet.service;

import com.mydiet.config.ClaudeApiClient;
import com.mydiet.model.*;
import com.mydiet.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClaudeService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private MealLogRepository mealLogRepository;
    
    @Autowired
    private EmotionLogRepository emotionLogRepository;
    
    @Autowired
    private WorkoutLogRepository workoutLogRepository;
    
    @Autowired
    private ClaudeResponseRepository claudeResponseRepository;

    @Autowired
    private ClaudeApiClient claudeApiClient;

    public String generateResponse(Long userId) {
        System.out.println("=== Claude 응답 생성 시작 - 사용자 ID: " + userId + " ===");
        
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            System.out.println("사용자 정보: " + user.getNickname() + ", 감정모드: " + user.getEmotionMode());
            
            LocalDate today = LocalDate.now();

            List<MealLog> meals = mealLogRepository.findByUserIdAndDate(userId, today);
            List<EmotionLog> emotions = emotionLogRepository.findByUserIdAndDate(userId, today);
            List<WorkoutLog> workouts = workoutLogRepository.findByUserIdAndDate(userId, today);

            System.out.println("오늘 데이터 - 식단: " + meals.size() + "개, 감정: " + emotions.size() + "개, 운동: " + workouts.size() + "개");

            String prompt = buildPrompt(user, meals, emotions, workouts);
            System.out.println("생성된 프롬프트: " + prompt);
            
            String response = claudeApiClient.askClaude(prompt);
            System.out.println("Claude 응답: " + response);

            try {
                ClaudeResponse log = new ClaudeResponse();
                log.setUser(user);
                log.setType("daily");
                log.setContent(response);
                log.setCreatedAt(LocalDateTime.now());
                claudeResponseRepository.save(log);
                System.out.println("Claude 응답 저장 완료");
            } catch (Exception e) {
                System.err.println("Claude 응답 저장 실패: " + e.getMessage());
            }

            return response;
        } catch (Exception e) {
            System.err.println("Claude 서비스 오류: " + e.getMessage());
            e.printStackTrace();
            return "오늘도 화이팅! 💪 목표를 향해 달려가세요!";
        }
    }

    private String buildPrompt(User user, List<MealLog> meals, List<EmotionLog> emotions, List<WorkoutLog> workouts) {
        StringBuilder prompt = new StringBuilder();
    
        prompt.append("당신은 다이어트 AI 코치입니다. 다음 정보를 바탕으로 한국어로 짧고 동기부여가 되는 한 문장을 만들어주세요.\n\n");
        prompt.append("사용자: ").append(user.getNickname()).append("\n");
        prompt.append("목표 체중: ").append(user.getWeightGoal()).append("kg\n");
        prompt.append("코치 스타일: ").append(user.getEmotionMode()).append("\n\n");
        
        if (meals.isEmpty()) {
            prompt.append("오늘 아직 식단 기록이 없습니다.\n");
        } else {
            prompt.append("오늘 식단: ");
            int totalCalories = 0;
            for (MealLog meal : meals) {
                prompt.append(meal.getDescription());
                if (meal.getCaloriesEstimate() != null) {
                    totalCalories += meal.getCaloriesEstimate();
                }
                prompt.append(", ");
            }
            prompt.append("(총 ").append(totalCalories).append("kcal)\n");
        }
        
        if (workouts.isEmpty()) {
            prompt.append("오늘 아직 운동 기록이 없습니다.\n");
        } else {
            prompt.append("오늘 운동: ");
            for (WorkoutLog workout : workouts) {
                prompt.append(workout.getType()).append(" ");
                prompt.append(workout.getDuration()).append("분, ");
            }
            prompt.append("\n");
        }
        
        prompt.append("\n위 정보를 바탕으로 ").append(user.getEmotionMode()).append(" 스타일로 동기부여하는 한 문장을 만들어주세요.");
        prompt.append("반말로, 이모지 포함해서 답변해주세요.");
    
        return prompt.toString();
    }
}