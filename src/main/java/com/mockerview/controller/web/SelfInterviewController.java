package com.mockerview.controller.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.Answer;
import com.mockerview.entity.SelfInterviewReport;
import com.mockerview.entity.User;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.SelfInterviewReportRepository;
import com.mockerview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Controller
@RequestMapping("/selfinterview")
@RequiredArgsConstructor
public class SelfInterviewController {

    private final UserRepository userRepository;
    private final SelfInterviewReportRepository selfInterviewReportRepository;
    private final AnswerRepository answerRepository;
    private final ObjectMapper objectMapper;

    @GetMapping("/create")
    public String createPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null) {
            User user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
            model.addAttribute("user", user);
        }
        return "selfinterview/create-ai";
    }

    @GetMapping("/reports")
    public String reportsList(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        List<SelfInterviewReport> reports = selfInterviewReportRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        
        double avgScore = reports.stream()
                .filter(r -> r.getOverallAvg() != null)
                .mapToDouble(SelfInterviewReport::getOverallAvg)
                .average()
                .orElse(0.0);
        
        int totalQuestions = reports.stream()
                .filter(r -> r.getTotalQuestions() != null)
                .mapToInt(SelfInterviewReport::getTotalQuestions)
                .sum();
        
        model.addAttribute("user", user);
        model.addAttribute("reports", reports);
        model.addAttribute("totalReports", reports.size());
        model.addAttribute("avgScore", avgScore);
        model.addAttribute("totalQuestions", totalQuestions);
        
        return "selfinterview/reports-list";
    }

    @GetMapping("/reports/{id}")
    public String reportDetail(@PathVariable Long id, 
                               @AuthenticationPrincipal CustomUserDetails userDetails, 
                               Model model) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        SelfInterviewReport report = selfInterviewReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("리포트를 찾을 수 없습니다."));
        
        if (!report.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("접근 권한이 없습니다.");
        }
        
        model.addAttribute("user", user);
        model.addAttribute("report", report);
        
        return "selfinterview/report-detail";
    }

    @GetMapping("/videos")
    public String videos(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        
        List<Map<String, Object>> allVideos = new ArrayList<>();
        
        List<SelfInterviewReport> reports = selfInterviewReportRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        for (SelfInterviewReport report : reports) {
            if (report.getVideoUrlsData() != null && !report.getVideoUrlsData().isEmpty()) {
                try {
                    Map<String, String> videoUrls = objectMapper.readValue(
                            report.getVideoUrlsData(), 
                            new TypeReference<Map<String, String>>() {}
                    );
                    
                    List<Map<String, Object>> questions = new ArrayList<>();
                    if (report.getQuestionsData() != null) {
                        questions = objectMapper.readValue(
                                report.getQuestionsData(),
                                new TypeReference<List<Map<String, Object>>>() {}
                        );
                    }
                    
                    for (Map.Entry<String, String> entry : videoUrls.entrySet()) {
                        String questionIndex = entry.getKey();
                        String videoUrl = entry.getValue();
                        
                        if (videoUrl != null && !videoUrl.isEmpty()) {
                            Map<String, Object> videoInfo = new HashMap<>();
                            videoInfo.put("videoUrl", videoUrl);
                            videoInfo.put("createdAt", report.getCreatedAt());
                            videoInfo.put("source", "셀프면접");
                            videoInfo.put("categoryName", report.getCategoryName());
                            
                            try {
                                int idx = Integer.parseInt(questionIndex);
                                if (idx < questions.size()) {
                                    Object questionText = questions.get(idx).get("question");
                                    videoInfo.put("questionText", questionText != null ? questionText.toString() : "질문 " + (idx + 1));
                                } else {
                                    videoInfo.put("questionText", "질문 " + (idx + 1));
                                }
                            } catch (NumberFormatException e) {
                                videoInfo.put("questionText", "질문");
                            }
                            
                            allVideos.add(videoInfo);
                        }
                    }
                } catch (Exception e) {
                    log.error("영상 URL 파싱 에러: {}", e.getMessage());
                }
            }
        }
        
        List<Answer> videoAnswers = answerRepository.findByUserIdAndVideoUrlIsNotNull(user.getId());
        for (Answer answer : videoAnswers) {
            Map<String, Object> videoInfo = new HashMap<>();
            videoInfo.put("videoUrl", answer.getVideoUrl());
            videoInfo.put("questionText", answer.getQuestion() != null ? answer.getQuestion().getText() : "질문");
            videoInfo.put("createdAt", answer.getCreatedAt());
            videoInfo.put("source", "일반면접");
            videoInfo.put("categoryName", answer.getQuestion() != null && answer.getQuestion().getSession() != null ? answer.getQuestion().getSession().getCategory() : "");
            allVideos.add(videoInfo);
        }
        
        allVideos.sort((a, b) -> {
            LocalDateTime dateA = (LocalDateTime) a.get("createdAt");
            LocalDateTime dateB = (LocalDateTime) b.get("createdAt");
            return dateB.compareTo(dateA);
        });
        
        model.addAttribute("user", user);
        model.addAttribute("allVideos", allVideos);
        model.addAttribute("totalVideos", allVideos.size());
        
        return "selfinterview/videos";
    }
}
