package com.mydiet.controller;

import com.mydiet.service.ClaudeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/claude")
@RequiredArgsConstructor
@Slf4j
public class ClaudeController {

    private final ClaudeService claudeService;

    @GetMapping("/daily-advice")
    public String getDailyAdvice(@RequestParam Long userId) {
        try {
            return claudeService.generateDailyAdvice(userId);
        } catch (Exception e) {
            log.error("일일 조언 생성 실패", e);
            return "오늘도 다이어트 화이팅! 꾸준함이 성공의 열쇠입니다! 💪✨";
        }
    }
    
    @GetMapping("/meal-advice")
    public String getMealAdvice(
            @RequestParam Long userId,
            @RequestParam String description,
            @RequestParam(required = false) Integer calories) {
        try {
            return claudeService.generateMealAdvice(userId, description, calories);
        } catch (Exception e) {
            log.error("식사 조언 생성 실패", e);
            return "좋은 식사 선택이에요! 건강한 식단을 유지하세요! 🍽️";
        }
    }
    
    @GetMapping("/workout-advice")
    public String getWorkoutAdvice(
            @RequestParam Long userId,
            @RequestParam String type,
            @RequestParam Integer duration) {
        try {
            return claudeService.generateWorkoutAdvice(userId, type, duration);
        } catch (Exception e) {
            log.error("운동 조언 생성 실패", e);
            return "훌륭한 운동이었어요! 계속 화이팅! 💪";
        }
    }
    
    @GetMapping("/weekly-analysis")
    public String getWeeklyAnalysis(@RequestParam Long userId) {
        try {
            return claudeService.generateWeeklyAnalysis(userId);
        } catch (Exception e) {
            log.error("주간 분석 생성 실패", e);
            return "이번 주도 정말 수고하셨어요! 꾸준한 노력이 멋진 결과를 만들어낼 거예요! 🌟💪";
        }
    }

    @GetMapping("/test-connection")
    public String testConnection() {
        try {
            String response = claudeService.generateDailyAdvice(1L);
            return "✅ Claude API 연결 성공! 응답: " + response;
        } catch (Exception e) {
            log.error("Claude 연결 테스트 실패", e);
            return "❌ Claude API 연결 실패: " + e.getMessage();
        }
    }
}
