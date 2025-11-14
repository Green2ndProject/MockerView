package com.mockerview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Slf4j
@Service
public class OpenAIService {

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openaiApiUrl;

    @Value("${openai.whisper.url:https://api.openai.com/v1/audio/transcriptions}")
    private String whisperApiUrl;

    @Value("${openai.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${openai.retry.delay-ms:1000}")
    private long retryDelayMs;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AtomicInteger requestCounter = new AtomicInteger(0);
    private final Map<String, Integer> tokenUsageTracker = new HashMap<>();

    private static final String DEFAULT_MODEL = "gpt-4o-mini";
    private static final String WHISPER_MODEL = "whisper-1";
    private static final double DEFAULT_TEMPERATURE = 0.7;

    private static final Pattern HALLUCINATION_PATTERN_1 = Pattern.compile("구독.*좋아요.*알람", Pattern.CASE_INSENSITIVE);
    private static final Pattern HALLUCINATION_PATTERN_2 = Pattern.compile("자막.*제공", Pattern.CASE_INSENSITIVE);
    private static final Pattern HALLUCINATION_PATTERN_3 = Pattern.compile("감사합니다\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern REPEATED_CHARS = Pattern.compile("(.)\\1{4,}");

    public OpenAIService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        
        this.restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().set("User-Agent", "MockerView/1.0");
            return execution.execute(request, body);
        });
    }

    public String generateFeedback(String questionText, String answerText, String category) {
        try {
            log.info("🧠 AI 피드백 생성 시작");
            validateInput(questionText, "질문");
            validateInput(answerText, "답변");

            String prompt = buildFeedbackPrompt(questionText, answerText, category);
            String result = generateTextWithRetry(prompt, 1000, DEFAULT_TEMPERATURE);
            
            log.info("✅ AI 피드백 생성 완료");
            return result;
        } catch (Exception e) {
            log.error("❌ AI 피드백 생성 실패", e);
            throw new RuntimeException("AI 피드백 생성 중 오류 발생: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> generateStructuredFeedback(String questionText, String answerText, String category) {
        try {
            log.info("🧠 구조화된 피드백 생성 시작");
            validateInput(questionText, "질문");
            validateInput(answerText, "답변");

            String prompt = buildStructuredFeedbackPrompt(questionText, answerText, category);
            String response = generateTextWithRetry(prompt, 1200, 0.5);
            response = cleanJsonResponse(response);
            Map<String, Object> result = parseAndValidateJson(response);
            validateFeedbackStructure(result);
            
            log.info("✅ 구조화된 피드백 완료");
            return result;
        } catch (Exception e) {
            log.error("❌ 구조화된 피드백 실패", e);
            return createFallbackFeedback();
        }
    }

    public Map<String, Object> analyzeInterviewPersonality(List<String> answers, String category) {
        try {
            log.info("🎭 면접 성향 분석 시작 - 답변 수: {}", answers.size());

            if (answers == null || answers.size() < 5) {
                throw new IllegalArgumentException("성향 분석을 위해 최소 5개의 답변이 필요합니다");
            }

            if (answers.size() > 20) {
                answers = answers.subList(0, 20);
            }

            String prompt = buildPersonalityAnalysisPrompt(answers, category);
            String response = generateTextWithRetry(prompt, 2000, 0.6);
            response = cleanJsonResponse(response);
            
            Map<String, Object> result = parseAndValidateJson(response);
            validatePersonalityStructure(result);
            
            String personalityType = determinePersonalityType(result);
            result.put("personality_type", personalityType);
            
            Map<String, Object> typeInfo = getPersonalityTypeInfo(personalityType);
            result.putAll(typeInfo);
            
            log.info("✅ 면접 성향 분석 완료 - 타입: {}", personalityType);
            return result;
        } catch (Exception e) {
            log.error("❌ 면접 성향 분석 실패", e);
            throw new RuntimeException("면접 성향 분석 중 오류 발생: " + e.getMessage(), e);
        }
    }

    public String generateQuestion(String category, String difficulty) {
        try {
            log.info("❓ AI 질문 생성");
            validateInput(category, "카테고리");
            validateInput(difficulty, "난이도");

            String prompt = buildQuestionPrompt(category, difficulty, 1);
            String result = generateTextWithRetry(prompt, 200, 0.8);
            result = cleanGeneratedQuestion(result);
            
            log.info("✅ AI 질문 생성 완료");
            return result;
        } catch (Exception e) {
            log.error("❌ AI 질문 생성 실패", e);
            return getFallbackQuestion(category);
        }
    }

    public List<String> generateMultipleQuestions(String category, String difficulty, int count) {
        try {
            log.info("❓ AI 복수 질문 생성 - count: {}", count);
            validateInput(category, "카테고리");
            validateInput(difficulty, "난이도");
            
            if (count < 1 || count > 20) {
                throw new IllegalArgumentException("질문 개수는 1-20개 사이여야 합니다");
            }

            String prompt = buildQuestionPrompt(category, difficulty, count);
            int maxTokens = Math.min(count * 100 + 200, 2000);
            String response = generateTextWithRetry(prompt, maxTokens, 0.8);
            List<String> questions = parseMultipleQuestions(response, count);
            
            log.info("✅ AI 복수 질문 생성 완료 - {}개", questions.size());
            return questions;
        } catch (Exception e) {
            log.error("❌ AI 복수 질문 생성 실패", e);
            return List.of(getFallbackQuestion(category));
        }
    }

    public String transcribeAudio(MultipartFile audioFile) {
        try {
            log.info("🎤 Whisper 음성 인식 시작");
            validateAudioFile(audioFile);

            File tempFile = createTempAudioFile(audioFile);
            
            try {
                String transcription = callWhisperAPI(tempFile);
                transcription = filterHallucinations(transcription);
                
                log.info("✅ Whisper 인식 완료");
                return transcription;
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }
        } catch (Exception e) {
            log.error("❌ Whisper 음성 인식 실패", e);
            return "";
        }
    }

    private String generateTextWithRetry(String prompt, int maxTokens, double temperature) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetryAttempts) {
            try {
                attempt++;
                String result = callOpenAIAPI(prompt, maxTokens, temperature);
                trackTokenUsage(prompt, result);
                return result;
            } catch (HttpClientErrorException e) {
                lastException = e;
                
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    log.warn("⚠️ Rate limit - 재시도 {}/{}", attempt, maxRetryAttempts);
                    sleep(retryDelayMs * attempt);
                    continue;
                } else if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    throw new RuntimeException("OpenAI API 키가 유효하지 않습니다", e);
                }
                throw new RuntimeException("OpenAI API 호출 실패", e);
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetryAttempts) {
                    sleep(retryDelayMs);
                    continue;
                }
                break;
            }
        }

        throw new RuntimeException("OpenAI API 호출 최종 실패", lastException);
    }

    private String callOpenAIAPI(String prompt, int maxTokens, double temperature) {
        int requestId = requestCounter.incrementAndGet();
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", DEFAULT_MODEL);
            requestBody.put("messages", List.of(message));
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                openaiApiUrl,
                HttpMethod.POST,
                entity,
                Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new RuntimeException("OpenAI 응답이 비어있습니다");
            }

            return extractContentFromResponse(responseBody);
        } catch (Exception e) {
            log.error("❌ [REQ-{}] OpenAI API 호출 실패", requestId, e);
            throw e;
        }
    }

    private String callWhisperAPI(File audioFile) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(openaiApiKey);

            org.springframework.util.LinkedMultiValueMap<String, Object> body = 
                new org.springframework.util.LinkedMultiValueMap<>();
            body.add("file", new org.springframework.core.io.FileSystemResource(audioFile));
            body.add("model", WHISPER_MODEL);
            body.add("language", "ko");
            body.add("temperature", 0.2);
            body.add("response_format", "json");

            HttpEntity<org.springframework.util.LinkedMultiValueMap<String, Object>> requestEntity = 
                new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                whisperApiUrl,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );

            if (response.getBody() != null && response.getBody().containsKey("text")) {
                return (String) response.getBody().get("text");
            }

            throw new RuntimeException("Whisper API 응답에 텍스트가 없습니다");
        } catch (Exception e) {
            log.error("❌ Whisper API 호출 실패", e);
            throw new RuntimeException("Whisper API 호출 중 오류 발생", e);
        }
    }

    private String buildFeedbackPrompt(String question, String answer, String category) {
        return String.format(
            "당신은 전문 면접관입니다. 다음 면접 답변을 평가해주세요.\n\n" +
            "【질문】\n%s\n\n【답변】\n%s\n\n【카테고리】%s\n\n" +
            "STAR 구조, 구체성, 논리성, 직무 적합성 기준으로 피드백해주세요.",
            question, answer, category
        );
    }

    private String buildStructuredFeedbackPrompt(String question, String answer, String category) {
        return String.format(
            "면접 답변을 STAR 기법으로 평가하여 JSON으로 응답하세요.\n\n" +
            "【질문】\n%s\n\n【답변】\n%s\n\n【카테고리】%s\n\n" +
            "JSON 형식:\n{\n" +
            "  \"summary\": \"평가 요약\",\n" +
            "  \"strengths\": \"강점\",\n" +
            "  \"weaknesses\": \"약점\",\n" +
            "  \"improvements\": \"개선 제안\",\n" +
            "  \"score\": 1-5,\n" +
            "  \"star_completeness\": 0-100,\n" +
            "  \"specificity\": 0-100,\n" +
            "  \"logic\": 0-100\n" +
            "}\n\nJSON만 출력하세요.",
            question, answer, category
        );
    }

    private String buildPersonalityAnalysisPrompt(List<String> answers, String category) {
        StringBuilder answersText = new StringBuilder();
        for (int i = 0; i < answers.size(); i++) {
            answersText.append(String.format("【답변 %d】\n%s\n\n", i + 1, answers.get(i)));
        }

        return String.format(
            "면접 답변들을 분석하여 성향을 8가지 차원에서 평가하세요.\n\n" +
            "【카테고리】%s\n\n%s\n" +
            "8가지 차원:\n" +
            "1. analytical_score vs creative_score (0-100)\n" +
            "2. logical_score vs emotional_score (0-100)\n" +
            "3. detail_oriented_score vs big_picture_score (0-100)\n" +
            "4. decisive_score vs flexible_score (0-100)\n\n" +
            "JSON 형식:\n{\n" +
            "  \"dimensions\": {\n" +
            "    \"analytical_score\": 0-100,\n" +
            "    \"creative_score\": 0-100,\n" +
            "    \"logical_score\": 0-100,\n" +
            "    \"emotional_score\": 0-100,\n" +
            "    \"detail_oriented_score\": 0-100,\n" +
            "    \"big_picture_score\": 0-100,\n" +
            "    \"decisive_score\": 0-100,\n" +
            "    \"flexible_score\": 0-100\n" +
            "  },\n" +
            "  \"analysis\": {\n" +
            "    \"thinking_style\": \"설명\",\n" +
            "    \"decision_making\": \"설명\",\n" +
            "    \"perspective\": \"설명\",\n" +
            "    \"execution\": \"설명\"\n" +
            "  },\n" +
            "  \"observed_strengths\": [\"강점1\", \"강점2\", \"강점3\"],\n" +
            "  \"development_areas\": [\"영역1\", \"영역2\", \"영역3\"],\n" +
            "  \"interview_approach\": \"접근 방식\",\n" +
            "  \"recommended_improvements\": [\"제안1\", \"제안2\", \"제안3\"]\n" +
            "}\n\nJSON만 출력하세요.",
            category, answersText.toString()
        );
    }

    private String buildQuestionPrompt(String category, String difficulty, int count) {
        if (count == 1) {
            return String.format(
                "전문 면접관으로서 STAR 기법 질문 1개를 생성하세요.\n" +
                "【카테고리】%s\n【난이도】%s\n\n질문만 작성하세요.",
                category, difficulty
            );
        } else {
            return String.format(
                "전문 면접관으로서 STAR 기법 질문을 %d개 생성하세요.\n" +
                "【카테고리】%s\n【난이도】%s\n\n" +
                "각 질문은 한 줄로, 번호 없이 작성하세요.",
                count, category, difficulty
            );
        }
    }

    private String determinePersonalityType(Map<String, Object> analysisResult) {
        Map<String, Object> dimensions = (Map<String, Object>) analysisResult.get("dimensions");
        
        int analyticalScore = getScore(dimensions, "analytical_score");
        int creativeScore = getScore(dimensions, "creative_score");
        int logicalScore = getScore(dimensions, "logical_score");
        int emotionalScore = getScore(dimensions, "emotional_score");
        int detailScore = getScore(dimensions, "detail_oriented_score");
        int bigPictureScore = getScore(dimensions, "big_picture_score");
        int decisiveScore = getScore(dimensions, "decisive_score");
        int flexibleScore = getScore(dimensions, "flexible_score");
        
        StringBuilder type = new StringBuilder();
        type.append(analyticalScore > creativeScore ? "A" : "C");
        type.append(logicalScore > emotionalScore ? "L" : "E");
        type.append(detailScore > bigPictureScore ? "D" : "B");
        type.append(decisiveScore > flexibleScore ? "S" : "F");
        
        return type.toString();
    }

    private int getScore(Map<String, Object> dimensions, String key) {
        Object value = dimensions.get(key);
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Double) return ((Double) value).intValue();
        if (value instanceof String) return Integer.parseInt((String) value);
        return 50;
    }

    private Map<String, Object> getPersonalityTypeInfo(String type) {
        Map<String, Object> typeInfo = new HashMap<>();
        
        switch (type) {
            case "ALDS":
                typeInfo.put("title", "체계적 분석가");
                typeInfo.put("subtitle", "데이터 기반 의사결정과 정밀한 실행력");
                typeInfo.put("traits", List.of("분석적", "논리적", "디테일", "결단력"));
                typeInfo.put("careers", List.of("데이터 분석가", "재무 분석가", "프로젝트 매니저"));
                typeInfo.put("tips", List.of("STAR 기법 활용", "구체적 숫자 제시", "체계적 답변"));
                break;
            
            case "ALDF":
                typeInfo.put("title", "정밀 연구자");
                typeInfo.put("subtitle", "논리적 분석과 꼼꼼한 검증");
                typeInfo.put("traits", List.of("분석적", "논리적", "디테일", "유연함"));
                typeInfo.put("careers", List.of("연구원", "품질관리 전문가", "감사"));
                typeInfo.put("tips", List.of("철저한 검증", "논리적 설명", "전문성 강조"));
                break;
            
            case "ALBS":
                typeInfo.put("title", "전략적 리더");
                typeInfo.put("subtitle", "큰 그림을 보며 빠르게 결정");
                typeInfo.put("traits", List.of("분석적", "논리적", "비전", "결단력"));
                typeInfo.put("careers", List.of("CEO", "전략 기획자", "경영 컨설턴트"));
                typeInfo.put("tips", List.of("비전 제시", "성과 강조", "리더십 경험"));
                break;
            
            case "ALBF":
                typeInfo.put("title", "유연한 전략가");
                typeInfo.put("subtitle", "장기 비전과 적응력");
                typeInfo.put("traits", List.of("분석적", "논리적", "비전", "유연함"));
                typeInfo.put("careers", List.of("정책 입안자", "조직 개발 전문가"));
                typeInfo.put("tips", List.of("적응력 강조", "다양한 관점", "상황별 전략"));
                break;
            
            case "AEDS":
                typeInfo.put("title", "관계 중심 실행가");
                typeInfo.put("subtitle", "공감 능력과 실행력");
                typeInfo.put("traits", List.of("분석적", "감성적", "디테일", "결단력"));
                typeInfo.put("careers", List.of("영업 관리자", "고객 성공 리더", "HR BP"));
                typeInfo.put("tips", List.of("팀워크 경험", "고객 지원 사례", "공감과 논리"));
                break;
            
            case "AEDF":
                typeInfo.put("title", "세심한 조력자");
                typeInfo.put("subtitle", "디테일한 관찰과 진심 어린 지원");
                typeInfo.put("traits", List.of("분석적", "감성적", "디테일", "유연함"));
                typeInfo.put("careers", List.of("상담사", "사회복지사", "HR 전문가"));
                typeInfo.put("tips", List.of("타인 지원 사례", "세심한 관찰력", "겸손한 강점 표현"));
                break;
            
            case "AEBS":
                typeInfo.put("title", "비전 관계자");
                typeInfo.put("subtitle", "사람과 비전을 연결");
                typeInfo.put("traits", List.of("분석적", "감성적", "비전", "결단력"));
                typeInfo.put("careers", List.of("사업 개발", "영업 전략가", "파트너십 매니저"));
                typeInfo.put("tips", List.of("관계 구축", "비전 제시", "팀 리더십"));
                break;
            
            case "AEBF":
                typeInfo.put("title", "성장 촉진자");
                typeInfo.put("subtitle", "유연한 접근으로 성장 지원");
                typeInfo.put("traits", List.of("분석적", "감성적", "비전", "유연함"));
                typeInfo.put("careers", List.of("HR 매니저", "조직문화 전문가", "리더십 코치"));
                typeInfo.put("tips", List.of("성장 지원 사례", "조직 문화 개선", "코칭 경험"));
                break;
            
            case "CLDS":
                typeInfo.put("title", "창의적 엔지니어");
                typeInfo.put("subtitle", "혁신적 아이디어를 정밀하게 구현");
                typeInfo.put("traits", List.of("창의적", "논리적", "디테일", "결단력"));
                typeInfo.put("careers", List.of("소프트웨어 개발자", "시스템 설계자", "엔지니어"));
                typeInfo.put("tips", List.of("기술 혁신 사례", "창의적 문제 해결", "기술 설명"));
                break;
            
            case "CLDF":
                typeInfo.put("title", "완벽주의 디자이너");
                typeInfo.put("subtitle", "창의성과 디테일의 조화");
                typeInfo.put("traits", List.of("창의적", "논리적", "디테일", "유연함"));
                typeInfo.put("careers", List.of("UX 디자이너", "제품 디자이너", "그래픽 디자이너"));
                typeInfo.put("tips", List.of("포트폴리오 준비", "창의적 과정 설명", "일정 준수 경험"));
                break;
            
            case "CLBS":
                typeInfo.put("title", "혁신 비전가");
                typeInfo.put("subtitle", "창의적 아이디어로 미래 제시");
                typeInfo.put("traits", List.of("창의적", "논리적", "비전", "결단력"));
                typeInfo.put("careers", List.of("크리에이티브 디렉터", "브랜드 전략가", "혁신 리더"));
                typeInfo.put("tips", List.of("혁신 사례", "비전과 실행", "트렌드 선도"));
                break;
            
            case "CLBF":
                typeInfo.put("title", "자유로운 창작자");
                typeInfo.put("subtitle", "제약 없이 새로운 것을 창조");
                typeInfo.put("traits", List.of("창의적", "논리적", "비전", "유연함"));
                typeInfo.put("careers", List.of("아티스트", "작가", "디자인 씽커"));
                typeInfo.put("tips", List.of("독창적 프로젝트", "창작 과정", "실험적 시도"));
                break;
            
            case "CEDS":
                typeInfo.put("title", "역동적 혁신가");
                typeInfo.put("subtitle", "빠른 실행으로 아이디어를 현실로");
                typeInfo.put("traits", List.of("창의적", "감성적", "디테일", "결단력"));
                typeInfo.put("careers", List.of("마케터", "콘텐츠 크리에이터", "스타트업 창업자"));
                typeInfo.put("tips", List.of("빠른 실행 사례", "창의적 실현", "열정과 책임감"));
                break;
            
            case "CEDF":
                typeInfo.put("title", "감성 창작자");
                typeInfo.put("subtitle", "감성과 창의성으로 영감을 제공");
                typeInfo.put("traits", List.of("창의적", "감성적", "디테일", "유연함"));
                typeInfo.put("careers", List.of("예술 치료사", "심리 상담사", "작가"));
                typeInfo.put("tips", List.of("감성적 작품", "긍정적 영향", "감성과 전문성"));
                break;
            
            case "CEBS":
                typeInfo.put("title", "변화 선도자");
                typeInfo.put("subtitle", "사회적 가치로 세상을 변화");
                typeInfo.put("traits", List.of("창의적", "감성적", "비전", "결단력"));
                typeInfo.put("careers", List.of("소셜 벤처", "비전 리더", "CSR 리더"));
                typeInfo.put("tips", List.of("사회적 가치", "변화 리더십", "이상과 현실"));
                break;
            
            case "CEBF":
                typeInfo.put("title", "조화로운 혁신가");
                typeInfo.put("subtitle", "공감과 변화를 균형있게");
                typeInfo.put("traits", List.of("창의적", "감성적", "비전", "유연함"));
                typeInfo.put("careers", List.of("비영리 활동가", "변화 관리자", "갈등 중재자"));
                typeInfo.put("tips", List.of("이해관계자 조율", "갈등 해결", "포용적 리더십"));
                break;
            
            default:
                typeInfo.put("title", "균형잡힌 실무자");
                typeInfo.put("subtitle", "다양한 역량을 갖춘 전문가");
                typeInfo.put("traits", List.of("균형", "실용", "적응력", "안정"));
                typeInfo.put("careers", List.of("프로젝트 매니저", "비즈니스 애널리스트"));
                typeInfo.put("tips", List.of("강점 명확화", "전문성 강화", "독특한 가치"));
                break;
        }
        
        return typeInfo;
    }

    private String extractContentFromResponse(Map<String, Object> responseBody) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return ((String) message.get("content")).trim();
    }

    private String filterHallucinations(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        
        String filtered = text;
        filtered = HALLUCINATION_PATTERN_1.matcher(filtered).replaceAll("");
        filtered = HALLUCINATION_PATTERN_2.matcher(filtered).replaceAll("");
        filtered = HALLUCINATION_PATTERN_3.matcher(filtered).replaceAll("");
        filtered = REPEATED_CHARS.matcher(filtered).replaceAll("$1$1");
        
        return filtered.trim();
    }

    private String cleanJsonResponse(String response) {
        return response
            .replaceAll("```json\\s*", "")
            .replaceAll("```\\s*", "")
            .replaceAll("^[^{]*", "")
            .replaceAll("[^}]*$", "")
            .trim();
    }

    private String cleanGeneratedQuestion(String question) {
        return question
            .replaceAll("^\\d+\\.\\s*", "")
            .replaceAll("^Q\\d*[:.]\\s*", "")
            .replaceAll("^질문[:.]\\s*", "")
            .replaceAll("^-\\s*", "")
            .trim();
    }

    private List<String> parseMultipleQuestions(String response, int expectedCount) {
        List<String> questions = new ArrayList<>();
        String[] lines = response.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            line = cleanGeneratedQuestion(line);
            if (line.length() > 10 && line.length() < 500) {
                questions.add(line);
            }
        }
        
        if (questions.isEmpty()) {
            questions.add(response.trim());
        }
        
        return questions;
    }

    private Map<String, Object> parseAndValidateJson(String jsonString) {
        try {
            return objectMapper.readValue(jsonString, Map.class);
        } catch (Exception e) {
            log.error("❌ JSON 파싱 실패", e);
            throw new RuntimeException("JSON 파싱 실패", e);
        }
    }

    private void validateFeedbackStructure(Map<String, Object> feedback) {
        if (!feedback.containsKey("score")) {
            feedback.put("score", 3);
        }
    }

    private void validatePersonalityStructure(Map<String, Object> personality) {
        if (!personality.containsKey("dimensions")) {
            throw new RuntimeException("성향 분석 필수 필드 누락: dimensions");
        }
    }

    private void validateInput(String input, String fieldName) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "이(가) 비어있습니다");
        }
    }

    private void validateAudioFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("오디오 파일이 비어있습니다");
        }
        if (file.getSize() < 5120) {
            throw new IllegalArgumentException("오디오 파일이 너무 작습니다");
        }
    }

    private File createTempAudioFile(MultipartFile audioFile) throws Exception {
        String originalFilename = audioFile.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") ?
            originalFilename.substring(originalFilename.lastIndexOf(".")) : ".webm";
        
        File tempFile = File.createTempFile("audio_", extension);
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(audioFile.getBytes());
        }
        return tempFile;
    }

    private Map<String, Object> createFallbackFeedback() {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("summary", "분석 불가");
        fallback.put("strengths", "분석 불가");
        fallback.put("weaknesses", "분석 불가");
        fallback.put("improvements", "시스템 오류");
        fallback.put("score", 3);
        fallback.put("star_completeness", 50);
        fallback.put("specificity", 50);
        fallback.put("logic", 50);
        return fallback;
    }

    private String getFallbackQuestion(String category) {
        return "어려운 문제 상황에서 주도적으로 해결책을 찾아 실행하고 성과를 낸 경험을 STAR 형식으로 말씀해주세요.";
    }

    private void trackTokenUsage(String prompt, String response) {
        int estimatedTokens = (prompt.length() + response.length()) / 4;
        String dateKey = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        tokenUsageTracker.merge(dateKey, estimatedTokens, Integer::sum);
    }

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public Map<String, Integer> getTokenUsageReport() {
        return new HashMap<>(tokenUsageTracker);
    }

    public void resetTokenUsageTracker() {
        tokenUsageTracker.clear();
    }

    public String generateText(String prompt, int maxTokens) {
        return generateTextWithRetry(prompt, maxTokens, DEFAULT_TEMPERATURE);
    }

    public String analyzeAnswer(String questionText, String answerText) {
        return generateFeedback(questionText, answerText, "GENERAL");
    }
}
