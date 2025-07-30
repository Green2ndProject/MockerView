package com.mydiet.controller;

import com.mydiet.model.WorkoutLog;
import com.mydiet.model.User;
import com.mydiet.repository.WorkoutLogRepository;
import com.mydiet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/workouts")
@RequiredArgsConstructor
public class WorkoutController {

    private final WorkoutLogRepository workoutLogRepository;
    private final UserRepository userRepository;

    @PostMapping
    public WorkoutLog addWorkout(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        User user = userRepository.findById(userId).orElseThrow();
        
        WorkoutLog.WorkoutType type = WorkoutLog.WorkoutType.valueOf(request.get("type").toString());
        Integer duration = Integer.valueOf(request.get("duration").toString());
        
        int caloriesBurned = calculateCalories(type, duration);
        
        WorkoutLog workout = WorkoutLog.builder()
            .user(user)
            .type(type)
            .duration(duration)
            .caloriesBurned(caloriesBurned)
            .build();
            
        return workoutLogRepository.save(workout);
    }

    @GetMapping("/today")
    public List<WorkoutLog> getTodayWorkouts(@RequestParam Long userId) {
        return workoutLogRepository.findByUserIdAndDate(userId, LocalDate.now());
    }

    @GetMapping("/range")
    public List<WorkoutLog> getWorkoutsByDateRange(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<WorkoutLog> allWorkouts = workoutLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return allWorkouts.stream()
            .filter(workout -> !workout.getDate().isBefore(startDate) && !workout.getDate().isAfter(endDate))
            .collect(Collectors.toList());
    }

    @DeleteMapping("/{id}")
    public void deleteWorkout(@PathVariable Long id) {
        workoutLogRepository.deleteById(id);
    }
    
    private int calculateCalories(WorkoutLog.WorkoutType type, int duration) {
        return switch (type) {
            case WALKING -> duration * 4;
            case RUNNING -> duration * 10;
            case CYCLING -> duration * 8;
            case SWIMMING -> duration * 12;
            case WEIGHT_TRAINING -> duration * 6;
            case YOGA -> duration * 3;
        };
    }
}
