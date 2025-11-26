package com.mockerview.controller.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockerview.entity.SelfInterviewReport;
import com.mockerview.entity.User;
import com.mockerview.repository.SelfInterviewReportRepository;
import com.mockerview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/self-interview/reports")
@RequiredArgsConstructor
@Slf4j
public class SelfInterviewReportController {

    private final SelfInterviewReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<SelfInterviewReport> reports = reportRepository.findByUserOrderByCreatedAtDesc(user);
        
        model.addAttribute("user", user);
        model.addAttribute("reports", reports);
        
        if (!reports.isEmpty()) {
            OptionalDouble avgOverall = reports.stream()
                    .filter(r -> r.getOverallAvg() != null)
                    .mapToDouble(SelfInterviewReport::getOverallAvg)
                    .average();
            
            int totalQuestions = reports.stream()
                    .mapToInt(SelfInterviewReport::getTotalQuestions)
                    .sum();
            
            OptionalDouble maxScore = reports.stream()
                    .filter(r -> r.getOverallAvg() != null)
                    .mapToDouble(SelfInterviewReport::getOverallAvg)
                    .max();
            
            model.addAttribute("avgOverall", avgOverall.orElse(0.0));
            model.addAttribute("totalQuestions", totalQuestions);
            model.addAttribute("maxScore", maxScore.orElse(0.0));
        } else {
            model.addAttribute("avgOverall", 0.0);
            model.addAttribute("totalQuestions", 0);
            model.addAttribute("maxScore", 0.0);
        }
        
        return "self-interview/report-list";
    }

    @GetMapping("/{reportId}")
    public String detail(@PathVariable Long reportId, 
                        @AuthenticationPrincipal UserDetails userDetails,
                        Model model) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        SelfInterviewReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        if (!report.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        try {
            List<String> questions = objectMapper.readValue(
                report.getQuestionsData(), 
                new TypeReference<List<String>>() {}
            );
            
            Map<String, Object> feedbacks = objectMapper.readValue(
                report.getFeedbacksData(),
                new TypeReference<Map<String, Object>>() {}
            );

            List<String> allStrengths = new ArrayList<>();
            List<String> allWeaknesses = new ArrayList<>();
            List<String> allImprovements = new ArrayList<>();
            List<Double> scores = new ArrayList<>();

            for (String key : feedbacks.keySet()) {
                Object feedbackObj = feedbacks.get(key);
                if (feedbackObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> feedbackMap = (Map<String, Object>) feedbackObj;
                    
                    for (String feedbackType : Arrays.asList("text", "audio", "video")) {
                        Object typedFeedback = feedbackMap.get(feedbackType);
                        if (typedFeedback instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> feedbackData = (Map<String, Object>) typedFeedback;
                            
                            if (feedbackData.get("score") != null) {
                                scores.add(Double.parseDouble(feedbackData.get("score").toString()));
                            }
                            
                            if (feedbackData.get("strengths") != null) {
                                allStrengths.add(feedbackData.get("strengths").toString());
                            }
                            
                            if (feedbackData.get("weaknesses") != null) {
                                allWeaknesses.add(feedbackData.get("weaknesses").toString());
                            }
                            
                            if (feedbackData.get("improvements") != null) {
                                allImprovements.add(feedbackData.get("improvements").toString());
                            }
                        }
                    }
                }
            }

            double avgScore = scores.isEmpty() ? 0.0 : scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double maxScore = scores.isEmpty() ? 0.0 : scores.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            double minScore = scores.isEmpty() ? 0.0 : scores.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            
            String performanceLevel = avgScore >= 4.0 ? "우수" : avgScore >= 3.0 ? "양호" : avgScore >= 2.0 ? "보통" : "개선 필요";
            
            Map<String, Object> analysisStats = new HashMap<>();
            analysisStats.put("avgScore", avgScore);
            analysisStats.put("maxScore", maxScore);
            analysisStats.put("minScore", minScore);
            analysisStats.put("totalAnswers", scores.size());
            analysisStats.put("performanceLevel", performanceLevel);
            
            String comprehensiveSummary = generateComprehensiveSummary(allStrengths, allWeaknesses, avgScore);
            List<String> keyStrengths = extractKeyPoints(allStrengths, 4);
            List<String> keyWeaknesses = extractKeyPoints(allWeaknesses, 4);
            List<String> actionPlans = generateActionPlans(allImprovements, allWeaknesses);

            model.addAttribute("user", user);
            model.addAttribute("report", report);
            model.addAttribute("questions", questions);
            model.addAttribute("analysisStats", analysisStats);
            model.addAttribute("comprehensiveSummary", comprehensiveSummary);
            model.addAttribute("keyStrengths", keyStrengths);
            model.addAttribute("keyWeaknesses", keyWeaknesses);
            model.addAttribute("actionPlans", actionPlans);
            
        } catch (Exception e) {
            log.error("Failed to parse report data", e);
            throw new RuntimeException("Failed to load report data");
        }

        return "self-interview/report-detail";
    }
    
    private String generateComprehensiveSummary(List<String> strengths, List<String> weaknesses, double avgScore) {
        StringBuilder summary = new StringBuilder();
        
        if (avgScore >= 4.0) {
            summary.append("전반적으로 매우 우수한 면접 성과를 보여주셨습니다. ");
        } else if (avgScore >= 3.0) {
            summary.append("전반적으로 양호한 면접 성과를 보여주셨습니다. ");
        } else if (avgScore >= 2.0) {
            summary.append("기본적인 면접 역량을 갖추고 있으나 개선의 여지가 있습니다. ");
        } else {
            summary.append("면접 역량 향상을 위해 집중적인 연습이 필요합니다. ");
        }
        
        if (!strengths.isEmpty()) {
            summary.append("특히 답변의 논리성과 명확성에서 강점을 보였으며, ");
        }
        
        if (!weaknesses.isEmpty()) {
            summary.append("답변의 깊이와 구체성을 보완한다면 더욱 경쟁력 있는 후보가 될 수 있습니다.");
        }
        
        return summary.toString();
    }
    
    private List<String> extractKeyPoints(List<String> points, int limit) {
        if (points.isEmpty()) {
            return Collections.emptyList();
        }
        
        return points.stream()
            .distinct()
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    private List<String> generateActionPlans(List<String> improvements, List<String> weaknesses) {
        List<String> plans = new ArrayList<>();
        
        if (!improvements.isEmpty()) {
            plans.addAll(improvements.stream().limit(3).collect(Collectors.toList()));
        }
        
        if (plans.isEmpty()) {
            plans.add("STAR 기법을 활용하여 답변을 구조화하는 연습을 하세요");
            plans.add("실제 면접 환경과 유사하게 시간을 재며 연습하세요");
            plans.add("녹화를 통해 자신의 답변을 객관적으로 분석하세요");
            plans.add("모의 면접을 통해 실전 감각을 기르세요");
        }
        
        return plans;
    }
}
