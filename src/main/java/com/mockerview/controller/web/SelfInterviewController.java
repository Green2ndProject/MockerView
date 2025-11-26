package com.mockerview.controller.web;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/selfinterview")
@RequiredArgsConstructor
public class SelfInterviewController {

    private final UserRepository userRepository;
    private final SelfInterviewReportRepository selfInterviewReportRepository;
    private final AnswerRepository answerRepository;
    private final ObjectMapper objectMapper;

    @GetMapping("/create-ai")
    public String createAI(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        try {
            User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow(() -> new RuntimeException("User not found"));
            model.addAttribute("user", user);
            return "selfinterview/create-ai";
        } catch (Exception e) {
            log.error("셀프 면접 페이지 로딩 실패: {}", e.getMessage(), e);
            return "redirect:/auth/mypage";
        }
    }

    @GetMapping("/reports/{reportId}")
    public String reportDetail(@PathVariable Long reportId, @AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        try {
            User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow(() -> new RuntimeException("User not found"));
            SelfInterviewReport report = selfInterviewReportRepository.findById(reportId).orElseThrow(() -> new RuntimeException("Report not found"));
            
            if (!report.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("권한이 없습니다");
            }
            
            model.addAttribute("user", user);
            model.addAttribute("report", report);
            
            return "selfinterview/report-detail";
        } catch (Exception e) {
            log.error("리포트 조회 실패: {}", e.getMessage(), e);
            return "redirect:/auth/mypage";
        }
    }

    @GetMapping("/videos")
    public String videos(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        try {
            User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow(() -> new RuntimeException("User not found"));
            
            List<Map<String, Object>> allVideos = new ArrayList<>();
            
            List<SelfInterviewReport> reports = selfInterviewReportRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
            for (SelfInterviewReport report : reports) {
                if (report.getVideoUrlsData() != null && !report.getVideoUrlsData().trim().isEmpty()) {
                    try {
                        Map<String, String> videoUrls = objectMapper.readValue(report.getVideoUrlsData(), Map.class);
                        
                        List<String> questions = objectMapper.readValue(report.getQuestionsData(), List.class);
                        
                        for (Map.Entry<String, String> entry : videoUrls.entrySet()) {
                            int index = Integer.parseInt(entry.getKey());
                            String videoUrl = entry.getValue();
                            
                            Map<String, Object> video = new HashMap<>();
                            video.put("videoUrl", videoUrl);
                            video.put("questionText", index < questions.size() ? questions.get(index) : "질문 없음");
                            video.put("createdAt", report.getCreatedAt());
                            video.put("reportId", report.getId());
                            video.put("categoryName", report.getCategoryName());
                            video.put("source", "셀프면접");
                            
                            allVideos.add(video);
                        }
                    } catch (Exception e) {
                        log.error("영상 데이터 파싱 실패 - reportId: {}", report.getId(), e);
                    }
                }
            }
            
            List<Answer> answerVideos = answerRepository.findByUserIdAndVideoUrlIsNotNull(user.getId());
            for (Answer answer : answerVideos) {
                Map<String, Object> video = new HashMap<>();
                video.put("videoUrl", answer.getVideoUrl());
                video.put("questionText", "일반면접 답변");
                video.put("createdAt", answer.getCreatedAt());
                video.put("answerId", answer.getId());
                video.put("source", "일반면접");
                
                allVideos.add(video);
            }
            
            allVideos.sort((v1, v2) -> ((java.time.LocalDateTime) v2.get("createdAt")).compareTo((java.time.LocalDateTime) v1.get("createdAt")));
            
            model.addAttribute("user", user);
            model.addAttribute("allVideos", allVideos);
            model.addAttribute("totalVideos", allVideos.size());
            
            return "selfinterview/videos";
        } catch (Exception e) {
            log.error("녹화 영상 목록 로딩 실패: {}", e.getMessage(), e);
            return "redirect:/auth/mypage";
        }
    }
}
