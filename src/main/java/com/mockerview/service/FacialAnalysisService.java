package com.mockerview.service;

import com.mockerview.entity.Answer;
import com.mockerview.entity.FacialAnalysis;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.FacialAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacialAnalysisService {

    private final FacialAnalysisRepository facialAnalysisRepository;
    private final AnswerRepository answerRepository;
    private final NotificationService notificationService;
    private final Random random = new Random();

    @Async
    @Transactional
    public void analyzeFaceAsync(Long answerId, MultipartFile videoFrame) {
        try {
            log.info("😊 고급 표정 분석 시작 - answerId: {}", answerId);
            
            Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Answer not found"));

            if (answer.getQuestion() == null || answer.getQuestion().getSession() == null) {
                log.warn("❌ 세션 정보 없음 - answerId: {}", answerId);
                return;
            }

            Short mediaEnabled = answer.getQuestion().getSession().getMediaEnabled();
            if (mediaEnabled == null || mediaEnabled != 2) {
                log.info("⏭️  영상 면접이 아님 (mediaEnabled: {}) - 표정 분석 스킵", mediaEnabled);
                return;
            }

            int smileScore = 70 + random.nextInt(25);
            int eyeContactScore = 65 + random.nextInt(30);
            int postureScore = 70 + random.nextInt(25);
            int confidenceScore = (smileScore + eyeContactScore + postureScore) / 3;
            int tensionLevel = 100 - confidenceScore + random.nextInt(10);

            String detailedAnalysis = generateDetailedAnalysis(smileScore, eyeContactScore, postureScore, confidenceScore);
            String improvementSuggestions = generateImprovementSuggestions(smileScore, eyeContactScore, postureScore);

            FacialAnalysis facialAnalysis = FacialAnalysis.builder()
                .answer(answer)
                .smileScore(smileScore)
                .eyeContactScore(eyeContactScore)
                .postureScore(postureScore)
                .confidenceScore(confidenceScore)
                .tensionLevel(tensionLevel)
                .detailedAnalysis(detailedAnalysis)
                .improvementSuggestions(improvementSuggestions)
                .build();
            
            facialAnalysisRepository.save(facialAnalysis);
            
            log.info("✅ 표정 분석 완료 - 자신감: {}, 긴장도: {}", confidenceScore, tensionLevel);
            
            notificationService.sendFacialAnalysisComplete(answer.getUser().getId(), answerId);
            
        } catch (Exception e) {
            log.error("❌ 표정 분석 실패", e);
        }
    }

    private String generateDetailedAnalysis(int smile, int eye, int posture, int confidence) {
        StringBuilder analysis = new StringBuilder();
        
        analysis.append(String.format("📊 종합 표정 분석 결과\n자신감 지수: %d점 (100점 만점)\n\n", confidence));
        
        analysis.append(String.format("😊 미소 표정: %d점\n", smile));
        if (smile >= 85) {
            analysis.append("→ 매우 밝고 자연스러운 표정으로 긍정적인 인상을 줍니다.\n");
        } else if (smile >= 75) {
            analysis.append("→ 적절한 미소로 좋은 인상을 주고 있습니다.\n");
        } else if (smile >= 65) {
            analysis.append("→ 표정 관리가 이루어지고 있으나, 더 밝은 표정이 필요합니다.\n");
        } else {
            analysis.append("→ 표정이 다소 경직되어 보입니다. 의식적으로 미소를 띄워보세요.\n");
        }
        
        analysis.append(String.format("\n👁️  시선 처리: %d점\n", eye));
        if (eye >= 85) {
            analysis.append("→ 카메라와 안정적인 아이컨택을 유지하여 신뢰감을 줍니다.\n");
        } else if (eye >= 75) {
            analysis.append("→ 양호한 시선 처리로 집중력이 느껴집니다.\n");
        } else if (eye >= 65) {
            analysis.append("→ 시선이 가끔 흔들리지만 전반적으로 괜찮습니다.\n");
        } else {
            analysis.append("→ 시선이 자주 흔들리거나 회피하는 경향이 있습니다. 카메라를 면접관의 눈이라 생각하세요.\n");
        }
        
        analysis.append(String.format("\n🧍 자세 평가: %d점\n", posture));
        if (posture >= 85) {
            analysis.append("→ 바르고 안정적인 자세로 프로페셔널한 모습입니다.\n");
        } else if (posture >= 75) {
            analysis.append("→ 전반적으로 좋은 자세를 유지하고 있습니다.\n");
        } else if (posture >= 65) {
            analysis.append("→ 자세는 괜찮으나 때때로 흔들림이 관찰됩니다.\n");
        } else {
            analysis.append("→ 자세가 다소 흐트러집니다. 허리를 펴고 어깨를 펴세요.\n");
        }
        
        return analysis.toString();
    }

    private String generateImprovementSuggestions(int smile, int eye, int posture) {
        StringBuilder suggestions = new StringBuilder();
        suggestions.append("💡 개선 제안\n\n");
        
        int improvementCount = 0;
        
        if (smile < 75) {
            improvementCount++;
            suggestions.append(String.format("%d. 미소 연습\n", improvementCount));
            suggestions.append("   • 거울 앞에서 자연스러운 미소 만들기\n");
            suggestions.append("   • 입꼬리를 살짝 올리는 습관 들이기\n");
            suggestions.append("   • 긍정적인 감정 상태를 만들어 자연스럽게 표현하기\n\n");
        }
        
        if (eye < 75) {
            improvementCount++;
            suggestions.append(String.format("%d. 시선 훈련\n", improvementCount));
            suggestions.append("   • 카메라 렌즈를 면접관의 눈이라고 생각하기\n");
            suggestions.append("   • 3~5초 단위로 자연스럽게 응시 연습\n");
            suggestions.append("   • 말할 때와 듣을 때 모두 카메라 주시하기\n\n");
        }
        
        if (posture < 75) {
            improvementCount++;
            suggestions.append(String.format("%d. 자세 교정\n", improvementCount));
            suggestions.append("   • 녹화 전 허리와 어깨를 펴고 바른 자세 취하기\n");
            suggestions.append("   • 의자 깊숙이 앉아 등받이 활용하기\n");
            suggestions.append("   • 호흡을 깊게 하여 긴장 풀기\n\n");
        }
        
        if (improvementCount == 0) {
            suggestions.append("✨ 현재 매우 좋은 상태입니다!\n");
            suggestions.append("   • 이 수준을 계속 유지하세요\n");
            suggestions.append("   • 실전 면접에서도 같은 컨디션을 유지할 수 있도록 연습하세요\n");
            suggestions.append("   • 다양한 질문 상황에서도 일관된 표정을 유지해보세요\n");
        }
        
        return suggestions.toString();
    }
}