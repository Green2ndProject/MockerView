package com.mydiet.mydiet.service;

import com.mydiet.config.ClaudeApiClient;
import com.mydiet.model.MealLog;
import com.mydiet.model.WorkoutLog;
import com.mydiet.model.User;
import com.mydiet.repository.MealLogRepository;
import com.mydiet.repository.WorkoutLogRepository;
import com.mydiet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaudeService {

    private final ClaudeApiClient claudeApiClient;
    private final UserRepository userRepository;
    private final MealLogRepository mealLogRepository;
    private final WorkoutLogRepository workoutLogRepository;

    public String generateDailyAdvice(Long userId) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
            
            List<MealLog> todayMeals = mealLogRepository.findByUserIdAndDate(userId, LocalDate.now());
            List<WorkoutLog> todayWorkouts = workoutLogRepository.findByUserIdAndDate(userId, LocalDate.now());
            
            String prompt = buildDailyPrompt(user, todayMeals, todayWorkouts);
            
            return claudeApiClient.askClaude(prompt, 0.9, 300);
            
        } catch (Exception e) {
            log.error("일일 조언 생성 실패", e);
            return generateFallbackAdvice();
        }
    }
    
    public String generateMealAdvice(Long userId, String mealDescription, Integer calories) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
            
            String prompt = buildMealPrompt(user, mealDescription, calories);
            
            return claudeApiClient.askClaude(prompt, 0.8, 200);
            
        } catch (Exception e) {
            log.error("식사 조언 생성 실패", e);
            return "훌륭한 식사 선택이네요! 균형 잡힌 식단을 유지하세요! 🍽️";
        }
    }
    
    public String generateWorkoutAdvice(Long userId, String workoutType, Integer duration) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
            
            String prompt = buildWorkoutPrompt(user, workoutType, duration);
            
            return claudeApiClient.askClaude(prompt, 0.7, 200);
            
        } catch (Exception e) {
            log.error("운동 조언 생성 실패", e);
            return "정말 좋은 운동이었어요! 꾸준함이 가장 중요합니다! 💪";
        }
    }
    
    public String generateWeeklyAnalysis(Long userId) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
            
            LocalDate weekAgo = LocalDate.now().minusDays(7);
            
            // 일주일 데이터 수집
            List<MealLog> weekMeals = mealLogRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(meal -> !meal.getDate().isBefore(weekAgo))
                .toList();
                
            List<WorkoutLog> weekWorkouts = workoutLogRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(workout -> !workout.getDate().isBefore(weekAgo))
                .toList();
            
            String prompt = buildWeeklyPrompt(user, weekMeals, weekWorkouts);
            
            return claudeApiClient.askClaude(prompt, 0.9, 500);
            
        } catch (Exception e) {
            log.error("주간 분석 생성 실패", e);
            return "이번 주도 수고하셨어요! 꾸준한 노력이 좋은 결과를 만들어낼 거예요! 💪✨";
        }
    }

    private String buildDailyPrompt(User user, List<MealLog> meals, List<WorkoutLog> workouts) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("당신은 친근하고 전문적인 다이어트 코치입니다. 다음 사용자의 오늘 하루를 분석하고 격려와 조언을 해주세요:\n\n");
        
        prompt.append("👤 사용자 정보:\n");
        prompt.append("- 닉네임: ").append(user.getNickname()).append("\n");
        if (user.getWeightGoal() != null) {
            prompt.append("- 목표 체중: ").append(user.getWeightGoal()).append("kg\n");
        }
        if (user.getCurrentWeight() != null) {
            prompt.append("- 현재 체중: ").append(user.getCurrentWeight()).append("kg\n");
        }
        prompt.append("\n");
        
        prompt.append("🍽️ 오늘의 식사 (").append(meals.size()).append("회):\n");
        if (meals.isEmpty()) {
            prompt.append("- 아직 기록된 식사가 없습니다.\n");
        } else {
            int totalCalories = 0;
            for (MealLog meal : meals) {
                prompt.append("- ").append(getMealTypeKorean(meal.getMealType()))
                      .append(": ").append(meal.getDescription());
                if (meal.getCaloriesEstimate() != null) {
                    prompt.append(" (").append(meal.getCaloriesEstimate()).append(" kcal)");
                    totalCalories += meal.getCaloriesEstimate();
                }
                prompt.append("\n");
            }
            prompt.append("총 섭취 칼로리: ").append(totalCalories).append(" kcal\n");
        }
        prompt.append("\n");
        
        prompt.append("🏃 오늘의 운동 (").append(workouts.size()).append("회):\n");
        if (workouts.isEmpty()) {
            prompt.append("- 아직 운동 기록이 없습니다.\n");
        } else {
            int totalBurned = 0;
            int totalDuration = 0;
            for (WorkoutLog workout : workouts) {
                prompt.append("- ").append(getWorkoutTypeKorean(workout.getType()))
                      .append(": ").append(workout.getDuration()).append("분");
                if (workout.getCaloriesBurned() != null) {
                    prompt.append(" (").append(workout.getCaloriesBurned()).append(" kcal 소모)");
                    totalBurned += workout.getCaloriesBurned();
                }
                totalDuration += workout.getDuration();
                prompt.append("\n");
            }
            prompt.append("총 운동시간: ").append(totalDuration).append("분, 총 소모 칼로리: ").append(totalBurned).append(" kcal\n");
        }
        prompt.append("\n");
        
        prompt.append("📝 요청사항:\n");
        prompt.append("위 정보를 바탕으로 친근하고 격려하는 톤으로 2-3문장의 개인화된 조언을 해주세요. ");
        prompt.append("구체적인 수치를 언급하며, 건설적인 피드백을 제공해주세요. ");
        prompt.append("이모지를 적절히 사용해서 재미있게 작성해주세요!");
        
        return prompt.toString();
    }
    
    private String buildMealPrompt(User user, String mealDescription, Integer calories) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("다이어트 코치로서 방금 기록된 식사에 대해 간단한 조언을 해주세요:\n\n");
        prompt.append("사용자: ").append(user.getNickname()).append("\n");
        prompt.append("식사: ").append(mealDescription).append("\n");
        if (calories != null) {
            prompt.append("칼로리: ").append(calories).append(" kcal\n");
        }
        prompt.append("\n한 문장으로 친근하게 피드백해주세요. 이모지 포함!");
        
        return prompt.toString();
    }
    
    private String buildWorkoutPrompt(User user, String workoutType, Integer duration) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("다이어트 코치로서 방금 완료된 운동에 대해 격려해주세요:\n\n");
        prompt.append("사용자: ").append(user.getNickname()).append("\n");
        prompt.append("운동: ").append(workoutType).append("\n");
        prompt.append("시간: ").append(duration).append("분\n");
        prompt.append("\n한 문장으로 열정적으로 격려해주세요. 이모지 포함!");
        
        return prompt.toString();
    }
    
    private String buildWeeklyPrompt(User user, List<MealLog> weekMeals, List<WorkoutLog> weekWorkouts) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("전문 다이어트 코치로서 일주일간의 데이터를 분석하고 종합적인 피드백을 해주세요:\n\n");
        
        prompt.append("👤 ").append(user.getNickname()).append("님의 지난 일주일:\n\n");
        
        prompt.append("📊 식사 분석:\n");
        prompt.append("- 총 식사 횟수: ").append(weekMeals.size()).append("회\n");
        if (!weekMeals.isEmpty()) {
            int totalCalories = weekMeals.stream()
                .mapToInt(meal -> meal.getCaloriesEstimate() != null ? meal.getCaloriesEstimate() : 0)
                .sum();
            prompt.append("- 총 섭취 칼로리: ").append(totalCalories).append(" kcal\n");
            prompt.append("- 일평균 칼로리: ").append(totalCalories / 7).append(" kcal\n");
        }
        
        prompt.append("\n💪 운동 분석:\n");
        prompt.append("- 총 운동 횟수: ").append(weekWorkouts.size()).append("회\n");
        if (!weekWorkouts.isEmpty()) {
            int totalDuration = weekWorkouts.stream()
                .mapToInt(WorkoutLog::getDuration)
                .sum();
            int totalBurned = weekWorkouts.stream()
                .mapToInt(workout -> workout.getCaloriesBurned() != null ? workout.getCaloriesBurned() : 0)
                .sum();
            prompt.append("- 총 운동시간: ").append(totalDuration).append("분\n");
            prompt.append("- 총 소모 칼로리: ").append(totalBurned).append(" kcal\n");
        }
        
        prompt.append("\n3-4문장으로 이번 주 성과를 분석하고, 다음 주 개선 방안을 제시해주세요. ");
        prompt.append("격려와 구체적인 조언을 포함해서 작성해주세요!");
        
        return prompt.toString();
    }
    
    private String generateFallbackAdvice() {
        String[] fallbackMessages = {
            "오늘도 다이어트 여정을 함께해서 기뻐요! 꾸준함이 가장 중요한 열쇠입니다! 💪✨",
            "건강한 선택들을 하고 계시는군요! 이런 습관이 좋은 결과를 만들어낼 거예요! 🌟",
            "매일 기록하는 습관이 정말 훌륭해요! 작은 변화들이 모여서 큰 성과가 됩니다! 🎯",
            "오늘 하루도 수고하셨어요! 자신을 믿고 계속 나아가세요! 🚀💚"
        };
        
        return fallbackMessages[(int) (Math.random() * fallbackMessages.length)];
    }
    
    private String getMealTypeKorean(MealLog.MealType mealType) {
        return switch (mealType) {
            case BREAKFAST -> "아침";
            case LUNCH -> "점심";
            case DINNER -> "저녁";
            case SNACK -> "간식";
        };
    }
    
    private String getWorkoutTypeKorean(WorkoutLog.WorkoutType workoutType) {
        return switch (workoutType) {
            case WALKING -> "걷기";
            case RUNNING -> "뛰기";
            case CYCLING -> "자전거";
            case SWIMMING -> "수영";
            case WEIGHT_TRAINING -> "웨이트";
            case YOGA -> "요가";
        };
    }
}
