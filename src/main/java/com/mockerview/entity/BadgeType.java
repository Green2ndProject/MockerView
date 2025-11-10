package com.mockerview.entity;

import lombok.Getter;

@Getter
public enum BadgeType {
    FIRST_INTERVIEW("첫 면접 완료", "첫 번째 면접을 완료했습니다", "🎯"),
    EARLY_BIRD("얼리버드", "오전 7시 전에 면접 진행", "🌅"),
    NIGHT_OWL("올빼미", "밤 11시 이후 면접 진행", "🦉"),
    PERFECT_SCORE("완벽한 면접", "90점 이상 획득", "💯"),
    INTERVIEW_10("10회 달성", "면접 10회 완료", "🏆"),
    INTERVIEW_50("50회 달성", "면접 50회 완료", "🎖️"),
    INTERVIEW_100("100회 달성", "면접 100회 완료", "👑"),
    STREAK_7("7일 연속", "7일 연속 면접 진행", "🔥"),
    STREAK_30("30일 연속", "30일 연속 면접 진행", "💎"),
    AI_MASTER("AI 마스터", "AI 피드백 100개 수령", "🤖"),
    QUICK_RESPONDER("빠른 응답자", "평균 답변 시간 30초 이하", "⚡"),
    DETAILED_ANSWER("상세 답변왕", "평균 답변 길이 200자 이상", "📝"),
    COMMUNICATION_PRO("커뮤니케이션 전문가", "커뮤니케이션 점수 95점 이상", "💬"),
    TECHNICAL_EXPERT("기술 전문가", "기술 점수 95점 이상", "💻"),
    CONFIDENT_SPEAKER("자신감 만렙", "자신감 점수 95점 이상", "🎤"),
    ALL_ROUNDER("올라운더", "모든 카테고리 면접 완료", "🌟");

    private final String displayName;
    private final String description;
    private final String emoji;

    BadgeType(String displayName, String description, String emoji) {
        this.displayName = displayName;
        this.description = description;
        this.emoji = emoji;
    }
}
