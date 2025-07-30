package com.mydiet.controller;

import com.mydiet.dto.MealRequest;
import com.mydiet.model.MealLog;
import com.mydiet.service.MealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meals")
public class MealController {

    @Autowired
    private MealService mealService;

    @PostMapping
    public ResponseEntity<?> saveMeal(@RequestBody MealRequest request) {
        MealLog saved = mealService.saveMeal(request);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/today")
    public ResponseEntity<?> getTodayMeals(@RequestParam Long userId) {
        return ResponseEntity.ok(mealService.getTodayMeals(userId));
    }
}
