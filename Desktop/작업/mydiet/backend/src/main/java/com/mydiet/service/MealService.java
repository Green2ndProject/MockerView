package com.mydiet.service;

import com.mydiet.dto.MealRequest;
import com.mydiet.model.MealLog;
import com.mydiet.model.User;
import com.mydiet.repository.MealLogRepository;
import com.mydiet.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class MealService {

    @Autowired
    private MealLogRepository mealLogRepository;
    
    @Autowired
    private UserRepository userRepository;

    public MealLog saveMeal(MealRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        MealLog mealLog = new MealLog();
        mealLog.setUser(user);
        mealLog.setDescription(request.getDescription());
        mealLog.setPhotoUrl(request.getPhotoUrl());
        mealLog.setCaloriesEstimate(request.getCaloriesEstimate());
        mealLog.setDate(request.getDate() != null ? request.getDate() : LocalDate.now());

        return mealLogRepository.save(mealLog);
    }

    public List<MealLog> getTodayMeals(Long userId) {
        return mealLogRepository.findByUserIdAndDate(userId, LocalDate.now());
    }
}
