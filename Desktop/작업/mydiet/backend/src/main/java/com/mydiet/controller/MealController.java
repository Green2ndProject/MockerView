package com.mydiet.controller;

import com.mydiet.model.MealLog;
import com.mydiet.model.User;
import com.mydiet.repository.MealLogRepository;
import com.mydiet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/meals")
@RequiredArgsConstructor
public class MealController {

    private final MealLogRepository mealLogRepository;
    private final UserRepository userRepository;

    @PostMapping
    public MealLog addMeal(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        User user = userRepository.findById(userId).orElseThrow();
        
        MealLog meal = MealLog.builder()
            .user(user)
            .description(request.get("description").toString())
            .caloriesEstimate(request.get("caloriesEstimate") != null ? 
                Integer.valueOf(request.get("caloriesEstimate").toString()) : null)
            .mealType(MealLog.MealType.valueOf(request.get("mealType").toString()))
            .build();
            
        return mealLogRepository.save(meal);
    }

    @GetMapping("/today")
    public List<MealLog> getTodayMeals(@RequestParam Long userId) {
        return mealLogRepository.findByUserIdAndDate(userId, LocalDate.now());
    }

    @GetMapping("/range")
    public List<MealLog> getMealsByDateRange(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<MealLog> allMeals = mealLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return allMeals.stream()
            .filter(meal -> !meal.getDate().isBefore(startDate) && !meal.getDate().isAfter(endDate))
            .collect(Collectors.toList());
    }

    @DeleteMapping("/{id}")
    public void deleteMeal(@PathVariable Long id) {
        mealLogRepository.deleteById(id);
    }
}
