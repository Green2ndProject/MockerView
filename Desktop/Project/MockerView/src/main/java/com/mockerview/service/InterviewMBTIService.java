package com.mockerview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockerview.entity.Answer;
import com.mockerview.entity.InterviewMBTI;
import com.mockerview.entity.User;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.InterviewMBTIRepository;
import com.mockerview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewMBTIService {

    private final InterviewMBTIRepository mbtiRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    
    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Transactional
    public InterviewMBTI analyzeMBTI(Long userId) {
        try {
            log.info("🧠 면접 MBTI 분석 시작 - userId: {}", userId);
            
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            List<Answer> userAnswers = answerRepository.findByUserIdOrderByCreatedAtDesc(userId);
            
            if (userAnswers.size() < 5) {
                throw new RuntimeException("MBTI 분석을 위해 최소 5개 이상의 답변이 필요합니다.");
            }

            String combinedAnswers = userAnswers.stream()
                .limit(20)
                .map(Answer::getAnswerText)
                .reduce("", (a, b) -> a + "\n\n" + b);

            Map<String, Integer> scores = analyzeWithGPT(combinedAnswers);
            String mbtiType = calculateMBTI(scores);
            
            InterviewMBTI mbti = InterviewMBTI.builder()
                .user(user)
                .mbtiType(mbtiType)
                .analyticalScore(scores.get("analytical"))
                .creativeScore(scores.get("creative"))
                .logicalScore(scores.get("logical"))
                .emotionalScore(scores.get("emotional"))
                .detailOrientedScore(scores.get("detailOriented"))
                .bigPictureScore(scores.get("bigPicture"))
                .decisiveScore(scores.get("decisive"))
                .flexibleScore(scores.get("flexible"))
                .strengthDescription(generateStrengthDescription(mbtiType, scores))
                .weaknessDescription(generateWeaknessDescription(mbtiType, scores))
                .careerRecommendation(generateCareerRecommendation(mbtiType))
                .build();
            
            mbtiRepository.save(mbti);
            
            log.info("✅ 면접 MBTI 분석 완료 - Type: {}", mbtiType);
            
            notificationService.sendMBTIAnalysisComplete(userId);
            
            return mbti;
            
        } catch (Exception e) {
            log.error("❌ MBTI 분석 실패", e);
            throw new RuntimeException("MBTI 분석 실패: " + e.getMessage());
        }
    }

    private Map<String, Integer> analyzeWithGPT(String answers) {
        try {
            String prompt = String.format(
                "다음은 면접 답변들입니다:\n\n%s\n\n" +
                "이 답변들을 분석하여 다음 8가지 축을 0-100점으로 평가하세요:\n" +
                "1. analytical: 분석적 사고 (데이터 기반, 논리적 분석)\n" +
                "2. creative: 창의적 사고 (새로운 아이디어, 독창성)\n" +
                "3. logical: 논리성 (체계적, 인과관계)\n" +
                "4. emotional: 감성적 (공감, 사람 중심)\n" +
                "5. detailOriented: 디테일 지향 (세부사항, 정확성)\n" +
                "6. bigPicture: 큰 그림 지향 (비전, 전략적)\n" +
                "7. decisive: 결단력 (빠른 의사결정, 단호함)\n" +
                "8. flexible: 유연성 (적응력, 열린 사고)\n\n" +
                "JSON 형식으로만 답변하세요.",
                answers.substring(0, Math.min(2000, answers.length()))
            );

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o-mini");
            requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "당신은 심리 분석 전문가입니다."),
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("max_tokens", 500);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + openaiApiKey);
            headers.set("Content-Type", "application/json");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                "https://api.openai.com/v1/chat/completions",
                entity,
                String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();
            
            content = content.replaceAll("```json", "").replaceAll("```", "").trim();
            
            JsonNode scoresNode = objectMapper.readTree(content);
            
            Map<String, Integer> scores = new HashMap<>();
            scores.put("analytical", scoresNode.path("analytical").asInt(50));
            scores.put("creative", scoresNode.path("creative").asInt(50));
            scores.put("logical", scoresNode.path("logical").asInt(50));
            scores.put("emotional", scoresNode.path("emotional").asInt(50));
            scores.put("detailOriented", scoresNode.path("detailOriented").asInt(50));
            scores.put("bigPicture", scoresNode.path("bigPicture").asInt(50));
            scores.put("decisive", scoresNode.path("decisive").asInt(50));
            scores.put("flexible", scoresNode.path("flexible").asInt(50));
            
            return scores;

        } catch (Exception e) {
            log.error("GPT 분석 실패", e);
            Map<String, Integer> defaultScores = new HashMap<>();
            defaultScores.put("analytical", 50);
            defaultScores.put("creative", 50);
            defaultScores.put("logical", 50);
            defaultScores.put("emotional", 50);
            defaultScores.put("detailOriented", 50);
            defaultScores.put("bigPicture", 50);
            defaultScores.put("decisive", 50);
            defaultScores.put("flexible", 50);
            return defaultScores;
        }
    }

    private String calculateMBTI(Map<String, Integer> scores) {
        StringBuilder mbti = new StringBuilder();
        
        mbti.append(scores.get("analytical") > scores.get("creative") ? "A" : "C");
        mbti.append(scores.get("logical") > scores.get("emotional") ? "L" : "E");
        mbti.append(scores.get("detailOriented") > scores.get("bigPicture") ? "D" : "B");
        mbti.append(scores.get("decisive") > scores.get("flexible") ? "S" : "F");
        
        return mbti.toString();
    }

    private String generateStrengthDescription(String mbtiType, Map<String, Integer> scores) {
        StringBuilder strengths = new StringBuilder();
        strengths.append("🌟 주요 강점\n\n");
        
        if (mbtiType.charAt(0) == 'A') {
            strengths.append("1. 분석적 사고력 (Analytical)\n");
            strengths.append("   • 데이터와 사실에 기반한 의사결정\n");
            strengths.append("   • 복잡한 문제를 체계적으로 분석\n");
            strengths.append("   • 논리적 근거를 명확히 제시\n\n");
        } else {
            strengths.append("1. 창의적 사고력 (Creative)\n");
            strengths.append("   • 독창적이고 혁신적인 아이디어 제시\n");
            strengths.append("   • 새로운 관점으로 문제 접근\n");
            strengths.append("   • 창의적 해결책 도출\n\n");
        }
        
        if (mbtiType.charAt(1) == 'L') {
            strengths.append("2. 논리적 문제해결 (Logical)\n");
            strengths.append("   • 체계적이고 단계적인 사고\n");
            strengths.append("   • 인과관계를 명확히 파악\n");
            strengths.append("   • 구조화된 의사소통\n\n");
        } else {
            strengths.append("2. 감성적 커뮤니케이션 (Emotional)\n");
            strengths.append("   • 높은 공감 능력과 이해력\n");
            strengths.append("   • 사람 중심의 의사결정\n");
            strengths.append("   • 팀원 동기부여에 능숙\n\n");
        }
        
        if (mbtiType.charAt(2) == 'D') {
            strengths.append("3. 디테일 지향성 (Detail-oriented)\n");
            strengths.append("   • 세부사항까지 꼼꼼히 확인\n");
            strengths.append("   • 정확성과 완성도 추구\n");
            strengths.append("   • 실수와 오류 최소화\n\n");
        } else {
            strengths.append("3. 큰 그림 사고 (Big-picture)\n");
            strengths.append("   • 전략적이고 비전 지향적\n");
            strengths.append("   • 장기적 관점의 의사결정\n");
            strengths.append("   • 통합적 시각으로 상황 파악\n\n");
        }
        
        if (mbtiType.charAt(3) == 'S') {
            strengths.append("4. 결단력 (Decisive)\n");
            strengths.append("   • 빠른 의사결정 능력\n");
            strengths.append("   • 명확하고 단호한 태도\n");
            strengths.append("   • 추진력과 실행력\n");
        } else {
            strengths.append("4. 유연성 (Flexible)\n");
            strengths.append("   • 높은 적응력과 융통성\n");
            strengths.append("   • 열린 마음으로 다양한 의견 수용\n");
            strengths.append("   • 변화에 유연하게 대응\n");
        }
        
        return strengths.toString();
    }

    private String generateWeaknessDescription(String mbtiType, Map<String, Integer> scores) {
        StringBuilder weaknesses = new StringBuilder();
        weaknesses.append("⚠️  보완이 필요한 영역\n\n");
        
        if (mbtiType.charAt(0) == 'A') {
            weaknesses.append("1. 창의성 개발\n");
            weaknesses.append("   • 기존 데이터에만 의존하는 경향\n");
            weaknesses.append("   • 새로운 시도에 신중할 수 있음\n");
            weaknesses.append("   • 브레인스토밍 활동 참여 권장\n\n");
        } else {
            weaknesses.append("1. 데이터 분석 역량\n");
            weaknesses.append("   • 직관에 의존하는 경향\n");
            weaknesses.append("   • 정량적 근거 부족 가능\n");
            weaknesses.append("   • 데이터 기반 의사결정 연습 필요\n\n");
        }
        
        if (mbtiType.charAt(1) == 'L') {
            weaknesses.append("2. 감성 지능 향상\n");
            weaknesses.append("   • 논리만 강조하여 공감 부족 가능\n");
            weaknesses.append("   • 인간관계에서 경직될 수 있음\n");
            weaknesses.append("   • 감정적 측면 고려 필요\n\n");
        } else {
            weaknesses.append("2. 논리적 구조화\n");
            weaknesses.append("   • 감정적 판단 우선 가능\n");
            weaknesses.append("   • 체계적 설명 부족할 수 있음\n");
            weaknesses.append("   • 논리적 프레임워크 학습 권장\n\n");
        }
        
        if (mbtiType.charAt(2) == 'D') {
            weaknesses.append("3. 전략적 시각 확대\n");
            weaknesses.append("   • 세부사항에 집중하다 큰 그림 놓칠 수 있음\n");
            weaknesses.append("   • 완벽주의로 시간 소요 가능\n");
            weaknesses.append("   • 우선순위 설정 연습 필요\n");
        } else {
            weaknesses.append("3. 실행력 강화\n");
            weaknesses.append("   • 큰 그림만 보고 실행 지연 가능\n");
            weaknesses.append("   • 디테일 놓칠 수 있음\n");
            weaknesses.append("   • 체크리스트 활용 권장\n");
        }
        
        return weaknesses.toString();
    }

    private String generateCareerRecommendation(String mbtiType) {
        Map<String, String[]> recommendationsMap = new HashMap<>();
        
        recommendationsMap.put("ALDS", new String[]{
            "데이터 분석가, 재무 분석가, 프로젝트 매니저",
            "정밀한 분석과 체계적 실행을 요구하는 직무에 강점",
            "금융, 컨설팅, IT 프로젝트 관리 분야 추천"
        });
        recommendationsMap.put("ALDF", new String[]{
            "연구원, 품질 관리 전문가, 시스템 분석가",
            "꼼꼼한 검증과 논리적 분석이 필요한 직무",
            "R&D, 품질보증, 학술 연구 분야 적합"
        });
        recommendationsMap.put("ALBS", new String[]{
            "전략 기획자, 경영 컨설턴트, CEO",
            "전략적 사고와 빠른 의사결정이 요구되는 리더십 포지션",
            "경영 전략, 사업 개발, 기업 임원 적합"
        });
        recommendationsMap.put("ALBF", new String[]{
            "정책 입안자, 교육 기획자, 조직 개발 전문가",
            "장기 비전과 유연한 접근이 필요한 직무",
            "공공정책, 교육, 조직 컨설팅 분야 추천"
        });
        recommendationsMap.put("AEDS", new String[]{
            "영업 관리자, 마케팅 디렉터, 고객 성공 리더",
            "사람 중심의 분석적 접근과 실행력",
            "B2B 영업, 마케팅 전략, CS 관리 적합"
        });
        recommendationsMap.put("AEDF", new String[]{
            "상담사, 사회복지사, HR 비즈니스 파트너",
            "공감 능력과 세심한 관찰이 필요한 직무",
            "심리상담, 복지, 인사 관리 분야 추천"
        });
        recommendationsMap.put("AEBS", new String[]{
            "사업 개발, 영업 전략가, 창업가",
            "관계 구축과 전략적 실행의 조화",
            "비즈니스 개발, 스타트업, 파트너십 관리"
        });
        recommendationsMap.put("AEBF", new String[]{
            "HR 매니저, 조직문화 전문가, 코치",
            "사람에 대한 이해와 유연한 조직 관리",
            "인사 기획, 조직문화, 리더십 코칭 적합"
        });
        recommendationsMap.put("CLDS", new String[]{
            "소프트웨어 개발자, 엔지니어, 시스템 설계자",
            "창의적 문제해결과 기술적 정밀성",
            "개발, 엔지니어링, 기술 아키텍처"
        });
        recommendationsMap.put("CLDF", new String[]{
            "UX 디자이너, 제품 디자이너, 건축가",
            "창의성과 디테일의 완벽한 조화",
            "디자인, 건축, 제품 개발 분야"
        });
        recommendationsMap.put("CLBS", new String[]{
            "크리에이티브 디렉터, 브랜드 전략가, 혁신 리더",
            "비전 제시와 창의적 실행력",
            "광고, 브랜딩, 혁신 전략 부서"
        });
        recommendationsMap.put("CLBF", new String[]{
            "아티스트, 작가, 디자인 씽커",
            "자유로운 창작과 실험적 접근",
            "예술, 문화 콘텐츠, 창작 분야"
        });
        recommendationsMap.put("CEDS", new String[]{
            "마케터, 콘텐츠 크리에이터, 스타트업 창업자",
            "창의적 아이디어와 빠른 실행",
            "마케팅, 콘텐츠 제작, 창업"
        });
        recommendationsMap.put("CEDF", new String[]{
            "예술 치료사, 심리 상담사, 창의적 교육자",
            "감성적 접근과 창의적 치유",
            "예술 치료, 대안 교육, 창의 상담"
        });
        recommendationsMap.put("CEBS", new String[]{
            "소셜 벤처 창업자, 비전 리더, 혁신 담당자",
            "사회적 가치와 창의적 변화 주도",
            "소셜벤처, 임팩트 비즈니스, 혁신 조직"
        });
        recommendationsMap.put("CEBF", new String[]{
            "비영리 활동가, 변화 관리자, 다양성 전문가",
            "공감과 변화를 이끄는 리더십",
            "비영리, 사회혁신, D&I 전문가"
        });
        
        String[] recommendation = recommendationsMap.getOrDefault(mbtiType, new String[]{
            "다양한 분야에서 활약 가능",
            "당신의 고유한 강점을 살릴 수 있는 분야 탐색",
            "흥미와 가치관에 맞는 직무 선택 권장"
        });
        
        StringBuilder result = new StringBuilder();
        result.append("💼 추천 직무\n\n");
        result.append(String.format("주요 직무: %s\n\n", recommendation[0]));
        result.append(String.format("강점 활용: %s\n\n", recommendation[1]));
        result.append(String.format("추천 분야: %s", recommendation[2]));
        
        return result.toString();
    }

    @Transactional(readOnly = true)
    public InterviewMBTI getLatestMBTI(Long userId) {
        return mbtiRepository.findLatestByUserId(userId)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<InterviewMBTI> getMBTIHistory(Long userId) {
        return mbtiRepository.findByUserId(userId);
    }

    public static String getMBTITypeDescription(String mbtiType) {
        Map<String, String> descriptions = new HashMap<>();
        
        descriptions.put("ALDS", "📊 체계적 분석가 (Systematic Analyst)\n데이터 기반 의사결정과 정밀한 실행력을 갖춘 전략적 실무자");
        descriptions.put("ALDF", "🔬 정밀 연구자 (Precise Researcher)\n논리적 분석과 꼼꼼한 검증으로 완벽을 추구하는 전문가");
        descriptions.put("ALBS", "🎯 전략적 리더 (Strategic Leader)\n큰 그림을 보며 빠르게 결정하는 비전 지향적 의사결정자");
        descriptions.put("ALBF", "🧭 유연한 전략가 (Flexible Strategist)\n장기 비전과 적응력을 겸비한 전략적 사고가");
        descriptions.put("AEDS", "🤝 관계 중심 실행가 (People-Oriented Executor)\n공감 능력과 실행력으로 팀을 이끄는 리더");
        descriptions.put("AEDF", "💚 세심한 조력자 (Caring Supporter)\n디테일한 관찰과 진심 어린 지원으로 타인을 돕는 전문가");
        descriptions.put("AEBS", "🌟 비전 관계자 (Visionary Connector)\n사람과 비전을 연결하며 변화를 만드는 리더");
        descriptions.put("AEBF", "🌱 성장 촉진자 (Growth Facilitator)\n유연한 접근으로 조직과 사람의 성장을 돕는 전문가");
        descriptions.put("CLDS", "⚙️  창의적 엔지니어 (Creative Engineer)\n혁신적 아이디어를 정밀하게 구현하는 기술 전문가");
        descriptions.put("CLDF", "🎨 완벽주의 디자이너 (Perfectionist Designer)\n창의성과 디테일의 조화로 완성도 높은 결과물 창출");
        descriptions.put("CLBS", "💡 혁신 비전가 (Innovation Visionary)\n창의적 아이디어로 새로운 미래를 제시하는 변화 주도자");
        descriptions.put("CLBF", "🎭 자유로운 창작자 (Free Creator)\n제약 없이 새로운 것을 탐구하고 창조하는 아티스트");
        descriptions.put("CEDS", "🚀 역동적 혁신가 (Dynamic Innovator)\n빠른 실행으로 창의적 아이디어를 현실로 만드는 실행가");
        descriptions.put("CEDF", "🎪 감성 창작자 (Empathetic Creator)\n감성과 창의성으로 사람들에게 영감을 주는 예술가");
        descriptions.put("CEBS", "🌈 변화 선도자 (Change Pioneer)\n사회적 가치와 혁신으로 세상을 변화시키는 리더");
        descriptions.put("CEBF", "🕊️  조화로운 혁신가 (Harmonious Innovator)\n공감과 변화를 균형있게 이끄는 포용적 리더");
        
        return descriptions.getOrDefault(mbtiType, "🌐 다재다능한 올라운더 (Versatile All-Rounder)\n다양한 분야에서 자신만의 강점을 발휘할 수 있는 인재");
    }

    public static String getMBTITypeLetter(char letter, int position) {
        Map<String, String> letterDescriptions = new HashMap<>();
        
        if (position == 0) {
            letterDescriptions.put("A", "분석적 (Analytical) - 데이터와 사실 기반 사고");
            letterDescriptions.put("C", "창의적 (Creative) - 혁신적이고 독창적 사고");
        } else if (position == 1) {
            letterDescriptions.put("L", "논리적 (Logical) - 체계적이고 합리적 접근");
            letterDescriptions.put("E", "감성적 (Emotional) - 공감과 사람 중심 접근");
        } else if (position == 2) {
            letterDescriptions.put("D", "디테일 (Detail) - 세부사항과 정확성 중시");
            letterDescriptions.put("B", "큰그림 (Big-picture) - 전략과 비전 중시");
        } else if (position == 3) {
            letterDescriptions.put("S", "결단력 (Decisive) - 빠르고 단호한 의사결정");
            letterDescriptions.put("F", "유연성 (Flexible) - 적응적이고 열린 태도");
        }
        
        return letterDescriptions.getOrDefault(String.valueOf(letter), "알 수 없음");
    }
}
