package com.mockerview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockerview.entity.*;
import com.mockerview.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewReportService {

    private final InterviewReportRepository reportRepository;
    private final SessionRepository sessionRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PDFGenerationService pdfGenerationService;

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${openai.api.url}")
    private String openaiApiUrl;

    @Async
    @Transactional
    public void generateReportAsync(Long sessionId) {
        try {
            Session session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다"));

            if (reportRepository.existsBySession(session)) {
                log.info("이미 리포트가 생성된 세션입니다: {}", sessionId);
                return;
            }

            InterviewReport report = generateReport(session);
            log.info("✅ 면접 리포트 생성 완료: Session {}", sessionId);
        } catch (Exception e) {
            log.error("❌ 리포트 생성 실패: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public InterviewReport generateReport(Session session) {
        log.info("🧠 면접 리포트 생성 시작: Session {}", session.getId());

        String aiInsights = generateAIInsights(session);
        Map<String, Object> insights = parseAIInsights(aiInsights);

        InterviewReport report = InterviewReport.builder()
                .session(session)
                .user(session.getHost())
                .overallScore((Integer) insights.getOrDefault("overallScore", 75))
                .overallInsights((String) insights.get("overallInsights"))
                .strengths((String) insights.get("strengths"))
                .weaknesses((String) insights.get("weaknesses"))
                .recommendations((String) insights.get("recommendations"))
                .detailedAnalysis((String) insights.get("detailedAnalysis"))
                .totalQuestions(session.getQuestions().size())
                .avgAnswerTime(0.0)
                .communicationScore((Integer) insights.getOrDefault("communicationScore", 75))
                .technicalScore((Integer) insights.getOrDefault("technicalScore", 75))
                .confidenceScore((Integer) insights.getOrDefault("confidenceScore", 75))
                .pdfGenerated(false)
                .build();

        report = reportRepository.save(report);

        String pdfUrl = pdfGenerationService.generatePDF(report);
        report.setPdfUrl(pdfUrl);
        report.setPdfGenerated(true);
        reportRepository.save(report);

        log.info("✅ 리포트 저장 완료: ID {}", report.getId());
        return report;
    }

    private String generateAIInsights(Session session) {
        String prompt = String.format("""
            당신은 전문 면접 코치입니다. 다음 면접 세션을 분석하여 종합 리포트를 작성해주세요.
            
            📊 면접 정보:
            - 총 질문 수: %d개
            - 세션 제목: %s
            
            다음 JSON 형식으로 응답해주세요:
            {
              "overallScore": 0-100 사이 점수,
              "communicationScore": 0-100 사이 점수,
              "technicalScore": 0-100 사이 점수,
              "confidenceScore": 0-100 사이 점수,
              "overallInsights": "전체적인 면접 평가 (200자 이내)",
              "strengths": "주요 강점 3가지 (각 50자 이내)",
              "weaknesses": "개선할 점 3가지 (각 50자 이내)",
              "recommendations": "구체적인 개선 방안 5가지 (각 100자 이내)",
              "detailedAnalysis": "상세 분석 (500자 이내)"
            }
            
            반드시 유효한 JSON만 반환하세요.
            """,
                session.getQuestions().size(),
                session.getTitle()
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o-mini");
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", "You are a professional interview coach. Always respond in valid JSON format."),
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(openaiApiUrl, request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();

            log.info("🧠 AI 인사이트 생성 완료");
            return content;
        } catch (Exception e) {
            log.error("❌ AI 인사이트 생성 실패: {}", e.getMessage());
            return getDefaultInsights();
        }
    }

    private Map<String, Object> parseAIInsights(String aiResponse) {
        try {
            String cleanJson = aiResponse.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            }
            if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substring(3);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }
            cleanJson = cleanJson.trim();

            return objectMapper.readValue(cleanJson, Map.class);
        } catch (Exception e) {
            log.error("❌ AI 응답 파싱 실패: {}", e.getMessage());
            return getDefaultInsightsMap();
        }
    }

    private String getDefaultInsights() {
        return """
            {
              "overallScore": 75,
              "communicationScore": 75,
              "technicalScore": 75,
              "confidenceScore": 75,
              "overallInsights": "전반적으로 안정적인 면접 수행을 보여주셨습니다.",
              "strengths": "명확한 의사소통, 논리적인 답변 구조, 적절한 답변 길이",
              "weaknesses": "구체적인 사례 부족, 답변 속도 개선 필요, 자신감 표현 부족",
              "recommendations": "실제 경험 사례를 더 많이 준비하세요, STAR 기법을 활용하세요, 모의 면접을 반복 연습하세요",
              "detailedAnalysis": "면접자는 전체적으로 준비된 모습을 보여주었습니다."
            }
            """;
    }

    private Map<String, Object> getDefaultInsightsMap() {
        Map<String, Object> insights = new HashMap<>();
        insights.put("overallScore", 75);
        insights.put("communicationScore", 75);
        insights.put("technicalScore", 75);
        insights.put("confidenceScore", 75);
        insights.put("overallInsights", "전반적으로 안정적인 면접 수행을 보여주셨습니다.");
        insights.put("strengths", "명확한 의사소통, 논리적인 답변 구조, 적절한 답변 길이");
        insights.put("weaknesses", "구체적인 사례 부족, 답변 속도 개선 필요, 자신감 표현 부족");
        insights.put("recommendations", "실제 경험 사례를 더 많이 준비하세요");
        insights.put("detailedAnalysis", "면접자는 전체적으로 준비된 모습을 보여주었습니다.");
        return insights;
    }

    @Transactional(readOnly = true)
    public List<InterviewReport> getUserReports(Long userId) {
        User user = new User();
        user.setId(userId);
        return reportRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public InterviewReport getReportBySession(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다"));
        return reportRepository.findBySession(session)
                .orElseThrow(() -> new RuntimeException("리포트를 찾을 수 없습니다"));
    }
}
