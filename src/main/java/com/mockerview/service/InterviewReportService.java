package com.mockerview.service;

import com.mockerview.entity.*;
import com.mockerview.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewReportService {

    private final InterviewReportRepository reportRepository;
    private final SessionRepository sessionRepository;
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final FeedbackRepository feedbackRepository;
    private final VoiceAnalysisRepository voiceAnalysisRepository;
    private final FacialAnalysisRepository facialAnalysisRepository;
    private final InterviewMBTIRepository mbtiRepository;
    private final OpenAIService openAIService;

    @Async
    @Transactional
    public void generateReportAsync(Long sessionId, Long userId) {
        log.info("📊 리포트 생성 시작 - sessionId: {}, userId: {}", sessionId, userId);
        
        InterviewReport report = null;
        
        try {
            Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다"));
            
            User user = session.getHost();
            if (user == null || !user.getId().equals(userId)) {
                throw new RuntimeException("리포트 생성 권한이 없습니다. HOST만 생성 가능합니다.");
            }

            if (session.getStatus() != Session.SessionStatus.ENDED) {
                throw new RuntimeException("종료된 세션만 리포트를 생성할 수 있습니다.");
            }

            report = InterviewReport.builder()
                .session(session)
                .generatedBy(user)
                .status(InterviewReport.ReportStatus.GENERATING)
                .build();
            
            report = reportRepository.save(report);
            log.info("✅ 리포트 레코드 생성 - reportId: {}", report.getId());

            Map<String, Object> reportData = analyzeSessionDetailed(sessionId);
            
            validateReportData(reportData);

            report.setTotalParticipants((Integer) reportData.get("totalParticipants"));
            report.setTotalQuestions((Integer) reportData.get("totalQuestions"));
            report.setTotalAnswers((Integer) reportData.get("totalAnswers"));
            report.setAverageScore((Double) reportData.get("averageScore"));
            report.setHighestScore((Integer) reportData.get("highestScore"));
            report.setLowestScore((Integer) reportData.get("lowestScore"));

            String aiGeneratedReport = generateDetailedAIReport(reportData, session);
            report.setReportContent(aiGeneratedReport);
            
            String summary = generateDetailedSummary(reportData, session);
            report.setSummary(summary);
            
            report.setStatus(InterviewReport.ReportStatus.COMPLETED);
            report.setCompletedAt(LocalDateTime.now());
            
            reportRepository.save(report);
            log.info("✅ 리포트 생성 완료 - reportId: {}, 소요시간: {}초", 
                report.getId(), 
                Duration.between(report.getCreatedAt(), report.getCompletedAt()).getSeconds());
            
        } catch (Exception e) {
            log.error("❌ 리포트 생성 실패 - sessionId: {}", sessionId, e);
            
            if (report != null) {
                report.setStatus(InterviewReport.ReportStatus.FAILED);
                report.setErrorMessage(truncateErrorMessage(e.getMessage()));
                report.setCompletedAt(LocalDateTime.now());
                reportRepository.save(report);
            }
            
            throw new RuntimeException("리포트 생성 중 오류 발생: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> analyzeSessionDetailed(Long sessionId) {
        log.info("🔍 세션 상세 분석 시작 - sessionId: {}", sessionId);
        
        Map<String, Object> data = new HashMap<>();
        
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다"));
        
        List<Answer> allAnswers = answerRepository.findBySessionIdOrderByCreatedAt(sessionId);
        List<Question> allQuestions = questionRepository.findBySessionId(sessionId);
        
        Set<Long> participantIds = allAnswers.stream()
            .map(a -> a.getUser().getId())
            .collect(Collectors.toSet());
        
        data.put("totalParticipants", participantIds.size());
        data.put("totalQuestions", allQuestions.size());
        data.put("totalAnswers", allAnswers.size());
        
        Map<Long, Map<String, Object>> participantStats = analyzeParticipants(allAnswers, participantIds);
        data.put("participantStats", participantStats);
        
        Map<String, Object> scoreAnalysis = analyzeScores(allAnswers);
        data.putAll(scoreAnalysis);
        
        Map<String, Object> timeAnalysis = analyzeTimeDistribution(allAnswers);
        data.put("timeAnalysis", timeAnalysis);
        
        Map<String, Object> questionAnalysis = analyzeQuestions(allQuestions, allAnswers);
        data.put("questionAnalysis", questionAnalysis);
        
        Map<String, Object> advancedAnalysis = analyzeAdvancedMetrics(allAnswers);
        data.put("advancedAnalysis", advancedAnalysis);
        
        data.put("sessionDuration", calculateSessionDuration(session));
        data.put("participationRate", calculateParticipationRate(participantIds.size(), allAnswers.size(), allQuestions.size()));
        
        log.info("✅ 세션 상세 분석 완료 - 참가자: {}, 질문: {}, 답변: {}", 
            participantIds.size(), allQuestions.size(), allAnswers.size());
        
        return data;
    }

    private Map<Long, Map<String, Object>> analyzeParticipants(List<Answer> allAnswers, Set<Long> participantIds) {
        Map<Long, Map<String, Object>> participantStats = new HashMap<>();
        
        for (Long participantId : participantIds) {
            List<Answer> participantAnswers = allAnswers.stream()
                .filter(a -> a.getUser().getId().equals(participantId))
                .collect(Collectors.toList());
            
            if (participantAnswers.isEmpty()) continue;
            
            User participant = participantAnswers.get(0).getUser();
            
            List<Feedback> participantFeedbacks = participantAnswers.stream()
                .flatMap(a -> a.getFeedbacks().stream())
                .collect(Collectors.toList());
            
            List<Integer> scores = participantFeedbacks.stream()
                .filter(f -> f.getScore() != null)
                .map(Feedback::getScore)
                .collect(Collectors.toList());
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("name", participant.getName());
            stats.put("userId", participantId);
            stats.put("answerCount", participantAnswers.size());
            
            if (!scores.isEmpty()) {
                double avgScore = scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
                int maxScore = scores.stream().mapToInt(Integer::intValue).max().orElse(0);
                int minScore = scores.stream().mapToInt(Integer::intValue).min().orElse(0);
                
                stats.put("averageScore", Math.round(avgScore * 10) / 10.0);
                stats.put("maxScore", maxScore);
                stats.put("minScore", minScore);
                stats.put("scoreStdDev", calculateStandardDeviation(scores));
            } else {
                stats.put("averageScore", 0.0);
                stats.put("maxScore", 0);
                stats.put("minScore", 0);
                stats.put("scoreStdDev", 0.0);
            }
            
            long aiFeedbackCount = participantFeedbacks.stream()
                .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.AI)
                .count();
            
            long humanFeedbackCount = participantFeedbacks.stream()
                .filter(f -> f.getFeedbackType() == Feedback.FeedbackType.INTERVIEWER)
                .count();
            
            stats.put("aiFeedbackCount", aiFeedbackCount);
            stats.put("humanFeedbackCount", humanFeedbackCount);
            
            List<VoiceAnalysis> userVoiceAnalyses = voiceAnalysisRepository.findByAnswerUserIdOrderByCreatedAtDesc(participantId);
            if (!userVoiceAnalyses.isEmpty()) {
                double avgVoiceStability = userVoiceAnalyses.stream()
                    .filter(v -> v.getVoiceStability() != null)
                    .mapToInt(VoiceAnalysis::getVoiceStability)
                    .average()
                    .orElse(0.0);
                stats.put("avgVoiceStability", Math.round(avgVoiceStability * 10) / 10.0);
            }
            
            List<InterviewMBTI> mbtiList = mbtiRepository.findByUserId(participantId);
            if (!mbtiList.isEmpty()) {
                stats.put("mbtiType", mbtiList.get(0).getMbtiType());
            }
            
            participantStats.put(participantId, stats);
        }
        
        return participantStats;
    }

    private Map<String, Object> analyzeScores(List<Answer> allAnswers) {
        Map<String, Object> scoreAnalysis = new HashMap<>();
        
        List<Feedback> allFeedbacks = allAnswers.stream()
            .flatMap(a -> a.getFeedbacks().stream())
            .collect(Collectors.toList());
        
        List<Integer> allScores = allFeedbacks.stream()
            .filter(f -> f.getScore() != null)
            .map(Feedback::getScore)
            .collect(Collectors.toList());
        
        if (allScores.isEmpty()) {
            scoreAnalysis.put("averageScore", 0.0);
            scoreAnalysis.put("highestScore", 0);
            scoreAnalysis.put("lowestScore", 0);
            scoreAnalysis.put("scoreDistribution", new HashMap<>());
            return scoreAnalysis;
        }
        
        double avgScore = allScores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        int maxScore = allScores.stream().mapToInt(Integer::intValue).max().orElse(0);
        int minScore = allScores.stream().mapToInt(Integer::intValue).min().orElse(0);
        
        scoreAnalysis.put("averageScore", Math.round(avgScore * 10) / 10.0);
        scoreAnalysis.put("highestScore", maxScore);
        scoreAnalysis.put("lowestScore", minScore);
        
        Map<Integer, Long> distribution = allScores.stream()
            .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        scoreAnalysis.put("scoreDistribution", distribution);
        
        scoreAnalysis.put("scoreStdDev", calculateStandardDeviation(allScores));
        
        double medianScore = calculateMedian(allScores);
        scoreAnalysis.put("medianScore", medianScore);
        
        return scoreAnalysis;
    }

    private Map<String, Object> analyzeTimeDistribution(List<Answer> allAnswers) {
        Map<String, Object> timeAnalysis = new HashMap<>();
        
        if (allAnswers.isEmpty()) {
            return timeAnalysis;
        }
        
        Map<String, Long> answersByHour = allAnswers.stream()
            .collect(Collectors.groupingBy(
                a -> a.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:00")),
                Collectors.counting()
            ));
        
        timeAnalysis.put("answersByHour", answersByHour);
        
        LocalDateTime firstAnswer = allAnswers.stream()
            .map(Answer::getCreatedAt)
            .min(LocalDateTime::compareTo)
            .orElse(null);
        
        LocalDateTime lastAnswer = allAnswers.stream()
            .map(Answer::getCreatedAt)
            .max(LocalDateTime::compareTo)
            .orElse(null);
        
        if (firstAnswer != null && lastAnswer != null) {
            long durationMinutes = Duration.between(firstAnswer, lastAnswer).toMinutes();
            timeAnalysis.put("totalDurationMinutes", durationMinutes);
            
            if (durationMinutes > 0) {
                double answersPerMinute = (double) allAnswers.size() / durationMinutes;
                timeAnalysis.put("answersPerMinute", Math.round(answersPerMinute * 100) / 100.0);
            }
        }
        
        return timeAnalysis;
    }

    private Map<String, Object> analyzeQuestions(List<Question> questions, List<Answer> answers) {
        Map<String, Object> questionAnalysis = new HashMap<>();
        
        Map<Long, Long> answerCountByQuestion = answers.stream()
            .collect(Collectors.groupingBy(
                a -> a.getQuestion().getId(),
                Collectors.counting()
            ));
        
        questionAnalysis.put("answerCountByQuestion", answerCountByQuestion);
        
        List<Map<String, Object>> questionStats = new ArrayList<>();
        for (Question q : questions) {
            Map<String, Object> qStat = new HashMap<>();
            qStat.put("questionId", q.getId());
            qStat.put("questionText", q.getText());
            qStat.put("answerCount", answerCountByQuestion.getOrDefault(q.getId(), 0L));
            
            List<Integer> questionScores = answers.stream()
                .filter(a -> a.getQuestion().getId().equals(q.getId()))
                .flatMap(a -> a.getFeedbacks().stream())
                .filter(f -> f.getScore() != null)
                .map(Feedback::getScore)
                .collect(Collectors.toList());
            
            if (!questionScores.isEmpty()) {
                double avgScore = questionScores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
                qStat.put("averageScore", Math.round(avgScore * 10) / 10.0);
            }
            
            questionStats.add(qStat);
        }
        
        questionAnalysis.put("questionStats", questionStats);
        
        return questionAnalysis;
    }

    private Map<String, Object> analyzeAdvancedMetrics(List<Answer> allAnswers) {
        Map<String, Object> advanced = new HashMap<>();
        
        Set<Long> userIds = allAnswers.stream()
            .map(a -> a.getUser().getId())
            .collect(Collectors.toSet());
        
        List<VoiceAnalysis> allVoiceAnalyses = new ArrayList<>();
        for (Long userId : userIds) {
            allVoiceAnalyses.addAll(voiceAnalysisRepository.findByAnswerUserIdOrderByCreatedAtDesc(userId));
        }
        
        if (!allVoiceAnalyses.isEmpty()) {
            double avgSpeed = allVoiceAnalyses.stream()
                .filter(v -> v.getSpeakingSpeed() != null)
                .mapToDouble(VoiceAnalysis::getSpeakingSpeed)
                .average()
                .orElse(0.0);
            
            double avgStability = allVoiceAnalyses.stream()
                .filter(v -> v.getVoiceStability() != null)
                .mapToInt(VoiceAnalysis::getVoiceStability)
                .average()
                .orElse(0.0);
            
            advanced.put("avgSpeakingSpeed", Math.round(avgSpeed * 10) / 10.0);
            advanced.put("avgVoiceStability", Math.round(avgStability * 10) / 10.0);
            advanced.put("voiceAnalysisCount", allVoiceAnalyses.size());
        }
        
        List<FacialAnalysis> allFacialAnalyses = new ArrayList<>();
        for (Long userId : userIds) {
            allFacialAnalyses.addAll(facialAnalysisRepository.findByAnswerUserIdOrderByCreatedAtDesc(userId));
        }
        
        if (!allFacialAnalyses.isEmpty()) {
            double avgSmile = allFacialAnalyses.stream()
                .filter(f -> f.getSmileScore() != null)
                .mapToInt(FacialAnalysis::getSmileScore)
                .average()
                .orElse(0.0);
            
            double avgConfidence = allFacialAnalyses.stream()
                .filter(f -> f.getConfidenceScore() != null)
                .mapToInt(FacialAnalysis::getConfidenceScore)
                .average()
                .orElse(0.0);
            
            advanced.put("avgSmileScore", Math.round(avgSmile * 10) / 10.0);
            advanced.put("avgConfidenceScore", Math.round(avgConfidence * 10) / 10.0);
            advanced.put("facialAnalysisCount", allFacialAnalyses.size());
        }
        
        long videoAnswerCount = allAnswers.stream()
            .filter(a -> a.getVideoUrl() != null && !a.getVideoUrl().trim().isEmpty())
            .count();
        
        advanced.put("videoAnswerCount", videoAnswerCount);
        
        return advanced;
    }

    private String calculateSessionDuration(Session session) {
        if (session.getStartTime() != null && session.getEndTime() != null) {
            Duration duration = Duration.between(session.getStartTime(), session.getEndTime());
            long hours = duration.toHours();
            long minutes = duration.toMinutes() % 60;
            return String.format("%d시간 %d분", hours, minutes);
        }
        return "미측정";
    }

    private double calculateParticipationRate(int participants, int totalAnswers, int totalQuestions) {
        if (participants == 0 || totalQuestions == 0) return 0.0;
        double expectedAnswers = participants * totalQuestions;
        double rate = (totalAnswers / expectedAnswers) * 100;
        return Math.round(rate * 10) / 10.0;
    }

    private double calculateStandardDeviation(List<Integer> scores) {
        if (scores.size() < 2) return 0.0;
        
        double mean = scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double variance = scores.stream()
            .mapToDouble(s -> Math.pow(s - mean, 2))
            .average()
            .orElse(0.0);
        
        return Math.round(Math.sqrt(variance) * 100) / 100.0;
    }

    private double calculateMedian(List<Integer> scores) {
        List<Integer> sorted = new ArrayList<>(scores);
        Collections.sort(sorted);
        
        int size = sorted.size();
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        } else {
            return sorted.get(size / 2);
        }
    }

    private void validateReportData(Map<String, Object> data) {
        if ((Integer) data.get("totalParticipants") == 0) {
            throw new RuntimeException("참가자가 없는 세션입니다");
        }
        
        if ((Integer) data.get("totalAnswers") == 0) {
            throw new RuntimeException("답변이 없는 세션입니다");
        }
    }

    private String generateDetailedAIReport(Map<String, Object> data, Session session) {
        try {
            log.info("🤖 AI 상세 리포트 생성 시작");
            
            StringBuilder prompt = new StringBuilder();
            prompt.append("# 면접 세션 상세 리포트 생성\n\n");
            prompt.append("## 세션 정보\n");
            prompt.append("- 제목: ").append(session.getTitle()).append("\n");
            prompt.append("- 설명: ").append(session.getDescription() != null ? session.getDescription() : "없음").append("\n");
            prompt.append("- 카테고리: ").append(session.getCategory()).append("\n");
            prompt.append("- 난이도: ").append(session.getDifficulty()).append("\n");
            prompt.append("- 진행 시간: ").append(data.get("sessionDuration")).append("\n\n");
            
            prompt.append("## 전체 통계\n");
            prompt.append("- 참가자 수: ").append(data.get("totalParticipants")).append("명\n");
            prompt.append("- 질문 수: ").append(data.get("totalQuestions")).append("개\n");
            prompt.append("- 답변 수: ").append(data.get("totalAnswers")).append("개\n");
            prompt.append("- 참여율: ").append(data.get("participationRate")).append("%\n");
            prompt.append("- 평균 점수: ").append(data.get("averageScore")).append("점\n");
            prompt.append("- 최고 점수: ").append(data.get("highestScore")).append("점\n");
            prompt.append("- 최저 점수: ").append(data.get("lowestScore")).append("점\n");
            prompt.append("- 점수 표준편차: ").append(data.get("scoreStdDev")).append("\n");
            prompt.append("- 중앙값: ").append(data.get("medianScore")).append("점\n\n");
            
            Map<Long, Map<String, Object>> participantStats = 
                (Map<Long, Map<String, Object>>) data.get("participantStats");
            
            prompt.append("## 참가자별 상세 분석\n");
            for (Map.Entry<Long, Map<String, Object>> entry : participantStats.entrySet()) {
                Map<String, Object> stats = entry.getValue();
                prompt.append("### ").append(stats.get("name")).append("\n");
                prompt.append("- 답변 수: ").append(stats.get("answerCount")).append("개\n");
                prompt.append("- 평균 점수: ").append(stats.get("averageScore")).append("점\n");
                prompt.append("- 최고/최저 점수: ").append(stats.get("maxScore"))
                    .append("/").append(stats.get("minScore")).append("점\n");
                prompt.append("- AI 피드백: ").append(stats.get("aiFeedbackCount")).append("개\n");
                prompt.append("- 면접관 피드백: ").append(stats.get("humanFeedbackCount")).append("개\n");
                if (stats.containsKey("avgVoiceStability")) {
                    prompt.append("- 음성 안정성: ").append(stats.get("avgVoiceStability")).append("\n");
                }
                if (stats.containsKey("mbtiType")) {
                    prompt.append("- MBTI: ").append(stats.get("mbtiType")).append("\n");
                }
                prompt.append("\n");
            }
            
            Map<String, Object> advanced = (Map<String, Object>) data.get("advancedAnalysis");
            if (advanced != null && !advanced.isEmpty()) {
                prompt.append("## 고급 분석 지표\n");
                if (advanced.containsKey("avgSpeakingSpeed")) {
                    prompt.append("- 평균 말하기 속도: ").append(advanced.get("avgSpeakingSpeed")).append("\n");
                }
                if (advanced.containsKey("avgVoiceStability")) {
                    prompt.append("- 평균 음성 안정성: ").append(advanced.get("avgVoiceStability")).append("\n");
                }
                if (advanced.containsKey("avgSmileScore")) {
                    prompt.append("- 평균 미소 점수: ").append(advanced.get("avgSmileScore")).append("\n");
                }
                if (advanced.containsKey("avgConfidenceScore")) {
                    prompt.append("- 평균 자신감 점수: ").append(advanced.get("avgConfidenceScore")).append("\n");
                }
                prompt.append("\n");
            }
            
            prompt.append("\n## 요청사항\n");
            prompt.append("위 데이터를 바탕으로 전문적인 면접 리포트를 Markdown 형식으로 작성해주세요.\n\n");
            prompt.append("포함할 내용:\n");
            prompt.append("1. **전체 요약** (Executive Summary)\n");
            prompt.append("2. **참가자별 평가 및 피드백**\n");
            prompt.append("3. **강점 분석**\n");
            prompt.append("4. **개선 영역**\n");
            prompt.append("5. **데이터 기반 인사이트**\n");
            prompt.append("6. **향후 권장사항**\n");
            
            return openAIService.generateText(prompt.toString(), 3000);
            
        } catch (Exception e) {
            log.error("❌ AI 리포트 생성 실패", e);
            return generateFallbackDetailedReport(data, session);
        }
    }

    private String generateFallbackDetailedReport(Map<String, Object> data, Session session) {
        StringBuilder report = new StringBuilder();
        report.append("# 면접 리포트\n\n");
        report.append("## 세션 정보\n");
        report.append("- **제목**: ").append(session.getTitle()).append("\n");
        report.append("- **카테고리**: ").append(session.getCategory()).append("\n");
        report.append("- **참가자 수**: ").append(data.get("totalParticipants")).append("명\n");
        report.append("- **질문 수**: ").append(data.get("totalQuestions")).append("개\n");
        report.append("- **답변 수**: ").append(data.get("totalAnswers")).append("개\n\n");
        
        report.append("## 통계 요약\n");
        report.append("- **평균 점수**: ").append(data.get("averageScore")).append("점\n");
        report.append("- **최고 점수**: ").append(data.get("highestScore")).append("점\n");
        report.append("- **최저 점수**: ").append(data.get("lowestScore")).append("점\n\n");
        
        report.append("## 참가자 분석\n");
        Map<Long, Map<String, Object>> participantStats = 
            (Map<Long, Map<String, Object>>) data.get("participantStats");
        
        for (Map<String, Object> stats : participantStats.values()) {
            report.append("### ").append(stats.get("name")).append("\n");
            report.append("- 답변: ").append(stats.get("answerCount")).append("개\n");
            report.append("- 평균 점수: ").append(stats.get("averageScore")).append("점\n\n");
        }
        
        return report.toString();
    }

    private String generateDetailedSummary(Map<String, Object> data, Session session) {
        return String.format(
            "%s | 참가자 %d명 | 질문 %d개 | 답변 %d개 | 평균 %.1f점 | 참여율 %.1f%%",
            session.getTitle(),
            data.get("totalParticipants"),
            data.get("totalQuestions"),
            data.get("totalAnswers"),
            data.get("averageScore"),
            data.get("participationRate")
        );
    }

    private String truncateErrorMessage(String message) {
        if (message == null) return "알 수 없는 오류";
        return message.length() > 1000 ? message.substring(0, 997) + "..." : message;
    }

    @Transactional(readOnly = true)
    public InterviewReport getReportById(Long reportId) {
        return reportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("리포트를 찾을 수 없습니다"));
    }

    @Transactional(readOnly = true)
    public List<InterviewReport> getReportsBySessionId(Long sessionId) {
        return reportRepository.findBySessionId(sessionId);
    }

    @Transactional(readOnly = true)
    public Optional<InterviewReport> getLatestCompletedReport(Long sessionId) {
        return reportRepository.findLatestCompletedBySessionId(sessionId);
    }

    @Transactional
    public void deleteReport(Long reportId, Long userId) {
        InterviewReport report = getReportById(reportId);
        
        if (!report.getGeneratedBy().getId().equals(userId) && 
            !report.getSession().getHost().getId().equals(userId)) {
            throw new RuntimeException("삭제 권한이 없습니다");
        }
        
        reportRepository.delete(report);
        log.info("🗑️ 리포트 삭제 완료 - reportId: {}", reportId);
    }

    @Transactional
    public void regenerateReport(Long reportId, Long userId) {
        InterviewReport existingReport = getReportById(reportId);
        
        if (!existingReport.getGeneratedBy().getId().equals(userId) && 
            !existingReport.getSession().getHost().getId().equals(userId)) {
            throw new RuntimeException("재생성 권한이 없습니다");
        }
        
        reportRepository.delete(existingReport);
        
        generateReportAsync(existingReport.getSession().getId(), userId);
        
        log.info("🔄 리포트 재생성 시작 - originalReportId: {}, sessionId: {}", 
            reportId, existingReport.getSession().getId());
    }
}
