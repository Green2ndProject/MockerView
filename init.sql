CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    username VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    role VARCHAR(255) NOT NULL,
    is_deleted INTEGER DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    withdrawal_reason VARCHAR(255)
);

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('STUDENT', 'HOST', 'REVIEWER', 'ADMIN'));

CREATE TABLE IF NOT EXISTS sessions (
    id BIGSERIAL PRIMARY KEY,
    host_id BIGINT REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    video_recording_url VARCHAR(500),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    status VARCHAR(255),
    session_type VARCHAR(255),
    is_reviewable VARCHAR(255),
    is_self_interview VARCHAR(255),
    agora_channel VARCHAR(255),
    media_enabled SMALLINT,
    last_activity TIMESTAMP,
    expires_at TIMESTAMP,
    difficulty VARCHAR(255),
    category VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ai_enabled BOOLEAN DEFAULT TRUE,
    ai_mode VARCHAR(20) DEFAULT 'FULL',
    ai_feedback_delay_seconds INTEGER DEFAULT 0,
    allow_participants_toggle_ai BOOLEAN DEFAULT FALSE
);

ALTER TABLE sessions DROP CONSTRAINT IF EXISTS sessions_status_check;
ALTER TABLE sessions ADD CONSTRAINT sessions_status_check CHECK (status IN ('PLANNED', 'RUNNING', 'ENDED'));

CREATE INDEX IF NOT EXISTS idx_sessions_expires ON sessions(expires_at);
CREATE INDEX IF NOT EXISTS idx_sessions_status_expires ON sessions(status, expires_at);

CREATE TABLE IF NOT EXISTS questions (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT REFERENCES sessions(id),
    question_text TEXT NOT NULL,
    order_no INT,
    questioner_id BIGINT REFERENCES users(id),
    timer INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS answers (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT REFERENCES questions(id) NOT NULL,
    user_id BIGINT REFERENCES users(id) NOT NULL,
    answer_text TEXT,
    audio_url TEXT,
    video_url TEXT,
    score INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ai_feedback_requested BOOLEAN DEFAULT TRUE,
    ai_feedback_generated BOOLEAN DEFAULT FALSE,
    ai_feedback_skipped_reason VARCHAR(255),
    ai_processing_time_ms BIGINT
);

CREATE INDEX IF NOT EXISTS idx_answers_video_url ON answers(video_url) WHERE video_url IS NOT NULL;
COMMENT ON COLUMN answers.video_url IS '답변 녹화 영상 URL (Cloudinary)';

CREATE TABLE IF NOT EXISTS feedbacks (
    id BIGSERIAL PRIMARY KEY,
    answer_id BIGINT REFERENCES answers(id) NOT NULL,
    summary TEXT,
    strengths TEXT,
    weaknesses TEXT,
    improvement_suggestions TEXT,
    model VARCHAR(50) DEFAULT 'GPT-4o-mini',
    feedback_type VARCHAR(50) NOT NULL,
    reviewer_id BIGINT REFERENCES users(id),
    reviewer_comment TEXT,
    score INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE feedbacks DROP CONSTRAINT IF EXISTS feedbacks_feedback_type_check;
ALTER TABLE feedbacks ADD CONSTRAINT feedbacks_feedback_type_check CHECK (feedback_type IN ('AI', 'INTERVIEWER'));

CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category_type VARCHAR(50),
    parent_id BIGINT REFERENCES categories(id),
    display_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    icon VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_categories_type ON categories(category_type);
CREATE INDEX IF NOT EXISTS idx_categories_parent ON categories(parent_id);

INSERT INTO categories (code, name, description, category_type, parent_id, display_order, is_active, icon) VALUES
('DEV', '개발', 'IT 및 소프트웨어 개발 분야', 'MAIN', NULL, 1, TRUE, '💻'),
('DESIGN', '디자인', 'UI/UX 및 그래픽 디자인', 'MAIN', NULL, 2, TRUE, '🎨'),
('BUSINESS', '기획/경영', '사업 기획 및 경영 전략', 'MAIN', NULL, 3, TRUE, '📊'),
('MARKETING', '마케팅', '마케팅 및 브랜딩', 'MAIN', NULL, 4, TRUE, '📢'),
('DATA', '데이터', '데이터 분석 및 AI', 'MAIN', NULL, 5, TRUE, '📈'),
('GENERAL', '일반', '공통 역량 및 인성', 'MAIN', NULL, 6, TRUE, '👥')
ON CONFLICT (code) DO NOTHING;

INSERT INTO categories (code, name, description, category_type, parent_id, display_order, is_active) VALUES
('DEV_FRONTEND', '프론트엔드', 'React, Vue, Angular 등', 'SUB', (SELECT id FROM categories WHERE code='DEV'), 1, TRUE),
('DEV_BACKEND', '백엔드', 'Java, Spring, Node.js 등', 'SUB', (SELECT id FROM categories WHERE code='DEV'), 2, TRUE),
('DEV_MOBILE', '모바일', 'Android, iOS 개발', 'SUB', (SELECT id FROM categories WHERE code='DEV'), 3, TRUE),
('DEV_DEVOPS', 'DevOps', 'CI/CD, 클라우드, 인프라', 'SUB', (SELECT id FROM categories WHERE code='DEV'), 4, TRUE),
('DESIGN_UI', 'UI 디자인', '사용자 인터페이스 디자인', 'SUB', (SELECT id FROM categories WHERE code='DESIGN'), 1, TRUE),
('DESIGN_UX', 'UX 디자인', '사용자 경험 디자인', 'SUB', (SELECT id FROM categories WHERE code='DESIGN'), 2, TRUE),
('DESIGN_GRAPHIC', '그래픽', '시각 디자인 및 브랜딩', 'SUB', (SELECT id FROM categories WHERE code='DESIGN'), 3, TRUE),
('BUSINESS_PLAN', '사업기획', '사업 전략 및 기획', 'SUB', (SELECT id FROM categories WHERE code='BUSINESS'), 1, TRUE),
('BUSINESS_PM', '프로덕트 매니저', '제품 관리 및 전략', 'SUB', (SELECT id FROM categories WHERE code='BUSINESS'), 2, TRUE),
('MARKETING_DIGITAL', '디지털 마케팅', 'SNS, 광고, SEO', 'SUB', (SELECT id FROM categories WHERE code='MARKETING'), 1, TRUE),
('MARKETING_CONTENT', '콘텐츠 마케팅', '콘텐츠 기획 및 제작', 'SUB', (SELECT id FROM categories WHERE code='MARKETING'), 2, TRUE),
('DATA_ANALYSIS', '데이터 분석', 'SQL, Python, 통계', 'SUB', (SELECT id FROM categories WHERE code='DATA'), 1, TRUE),
('DATA_ML', '머신러닝/AI', '머신러닝 및 딥러닝', 'SUB', (SELECT id FROM categories WHERE code='DATA'), 2, TRUE)
ON CONFLICT (code) DO NOTHING;

CREATE TABLE IF NOT EXISTS question_pool (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    difficulty VARCHAR(20),
    question_text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reviews (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT REFERENCES sessions(id),
    reviewer_id BIGINT REFERENCES users(id),
    answer_id BIGINT REFERENCES answers(id),
    review_comment TEXT,
    rating DOUBLE PRECISION,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    plan_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    next_billing_date TIMESTAMP,
    auto_renew BOOLEAN DEFAULT TRUE,
    session_limit INTEGER,
    used_sessions INTEGER DEFAULT 0,
    review_read_limit INTEGER,
    used_review_reads INTEGER DEFAULT 0,
    payment_method_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    subscription_id BIGINT REFERENCES subscriptions(id),
    order_id VARCHAR(255),
    payment_key VARCHAR(255),
    transaction_id VARCHAR(255),
    method VARCHAR(20),
    status VARCHAR(20),
    amount DECIMAL(10,2),
    currency VARCHAR(10) DEFAULT 'KRW',
    requested_at TIMESTAMP,
    approved_at TIMESTAMP,
    failed_at TIMESTAMP,
    failure_reason TEXT,
    receipt_url TEXT
);

CREATE TABLE IF NOT EXISTS push_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) NOT NULL,
    endpoint VARCHAR(500) NOT NULL UNIQUE,
    p256dh VARCHAR(500) NOT NULL,
    auth VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_used TIMESTAMP,
    active BOOLEAN DEFAULT TRUE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_push_subscriptions_user ON push_subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_push_subscriptions_active ON push_subscriptions(active);

INSERT INTO question_pool (category, difficulty, question_text, created_at) 
SELECT '기술', 'EASY', 'Java의 JVM 구조에 대해 설명해주세요.', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM question_pool WHERE question_text = 'Java의 JVM 구조에 대해 설명해주세요.');

INSERT INTO question_pool (category, difficulty, question_text, created_at) 
SELECT '기술', 'MEDIUM', 'Spring과 Spring Boot의 차이점은 무엇인가요?', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM question_pool WHERE question_text = 'Spring과 Spring Boot의 차이점은 무엇인가요?');

INSERT INTO question_pool (category, difficulty, question_text, created_at) 
SELECT '기술', 'HARD', 'RESTful API 설계 원칙과 실제 프로젝트 적용 경험을 말씀해주세요.', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM question_pool WHERE question_text LIKE '%RESTful API 설계 원칙%');

INSERT INTO question_pool (category, difficulty, question_text, created_at) 
SELECT '인성', 'EASY', '자신의 장점과 단점을 말씀해주세요.', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM question_pool WHERE question_text = '자신의 장점과 단점을 말씀해주세요.');

INSERT INTO question_pool (category, difficulty, question_text, created_at) 
SELECT '인성', 'MEDIUM', '팀 프로젝트에서 갈등을 해결한 경험이 있나요?', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM question_pool WHERE question_text LIKE '%갈등을 해결한 경험%');

INSERT INTO question_pool (category, difficulty, question_text, created_at) 
SELECT '상황', 'MEDIUM', '마감 기한이 촉박한 상황에서 어떻게 대처하시나요?', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM question_pool WHERE question_text LIKE '%마감 기한이 촉박%');

INSERT INTO question_pool (category, difficulty, question_text, created_at) 
SELECT '상황', 'HARD', '기술적 의견 충돌이 있을 때 어떻게 해결하시나요?', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM question_pool WHERE question_text LIKE '%기술적 의견 충돌%');

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM users LIMIT 1) THEN
        INSERT INTO subscriptions (user_id, plan_type, status, start_date, end_date, next_billing_date, session_limit, used_sessions, review_read_limit, used_review_reads, auto_renew, created_at, updated_at)
        SELECT 
            u.id,
            'FREE',
            'ACTIVE',
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP + INTERVAL '1 month',
            CURRENT_TIMESTAMP + INTERVAL '1 month',
            5,
            0,
            3,
            0,
            TRUE,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        FROM users u
        WHERE NOT EXISTS (
            SELECT 1 FROM subscriptions s WHERE s.user_id = u.id AND s.status = 'ACTIVE'
        );
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS interviewer_notes (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    interviewer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    interviewee_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating INTEGER,
    strengths VARCHAR(1000),
    weaknesses VARCHAR(1000),
    improvements VARCHAR(1000),
    overall_comment VARCHAR(2000),
    notes VARCHAR(2000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    submitted BOOLEAN DEFAULT FALSE,
    CONSTRAINT unique_interviewer_interviewee_session UNIQUE (session_id, interviewer_id, interviewee_id)
);

CREATE INDEX IF NOT EXISTS idx_interviewer_notes_session ON interviewer_notes(session_id);
CREATE INDEX IF NOT EXISTS idx_interviewer_notes_interviewer ON interviewer_notes(interviewer_id);
CREATE INDEX IF NOT EXISTS idx_interviewer_notes_interviewee ON interviewer_notes(interviewee_id);

CREATE TABLE IF NOT EXISTS interview_reports (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT REFERENCES sessions(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    report_type VARCHAR(20) NOT NULL,
    pdf_url VARCHAR(500),
    share_link VARCHAR(255) UNIQUE,
    share_password VARCHAR(255),
    link_expires_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'GENERATING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_interview_reports_session ON interview_reports(session_id);
CREATE INDEX IF NOT EXISTS idx_interview_reports_user ON interview_reports(user_id);
CREATE INDEX IF NOT EXISTS idx_interview_reports_share_link ON interview_reports(share_link);

ALTER TABLE interview_reports DROP CONSTRAINT IF EXISTS interview_reports_report_type_check;
ALTER TABLE interview_reports ADD CONSTRAINT interview_reports_report_type_check CHECK (report_type IN ('INTERVIEWEE', 'INTERVIEWER'));

ALTER TABLE interview_reports DROP CONSTRAINT IF EXISTS interview_reports_status_check;
ALTER TABLE interview_reports ADD CONSTRAINT interview_reports_status_check CHECK (status IN ('GENERATING', 'COMPLETED', 'FAILED'));

CREATE TABLE IF NOT EXISTS report_comparisons (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    report_id_1 BIGINT REFERENCES interview_reports(id) ON DELETE CASCADE,
    report_id_2 BIGINT REFERENCES interview_reports(id) ON DELETE CASCADE,
    growth_rate DECIMAL(5,2),
    comparison_data TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_report_comparisons_user ON report_comparisons(user_id);

CREATE TABLE IF NOT EXISTS candidate_resumes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE UNIQUE,
    resume_pdf_url VARCHAR(500),
    portfolio_url VARCHAR(500),
    education VARCHAR(1000),
    experience VARCHAR(2000),
    tech_stack VARCHAR(1000),
    preferred_salary VARCHAR(100),
    preferred_location VARCHAR(255),
    is_public BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_candidate_resumes_user ON candidate_resumes(user_id);
CREATE INDEX IF NOT EXISTS idx_candidate_resumes_public ON candidate_resumes(is_public);

CREATE TABLE IF NOT EXISTS talent_pool (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    candidate_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    tags VARCHAR(500),
    recruiter_note VARCHAR(1000),
    status VARCHAR(50) DEFAULT 'INTERESTED',
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_talent_pool_company ON talent_pool(company_id);
CREATE INDEX IF NOT EXISTS idx_talent_pool_candidate ON talent_pool(candidate_id);
CREATE INDEX IF NOT EXISTS idx_talent_pool_status ON talent_pool(status);

ALTER TABLE talent_pool DROP CONSTRAINT IF EXISTS talent_pool_status_check;
ALTER TABLE talent_pool ADD CONSTRAINT talent_pool_status_check CHECK (status IN ('INTERESTED', 'CONTACTED', 'INTERVIEWING', 'OFFERED', 'HIRED', 'REJECTED'));

CREATE TABLE IF NOT EXISTS interviewer_interests (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT REFERENCES sessions(id) ON DELETE CASCADE,
    interviewer_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    candidate_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    marked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_session_interviewer_candidate UNIQUE (session_id, interviewer_id, candidate_id)
);

CREATE INDEX IF NOT EXISTS idx_interviewer_interests_session ON interviewer_interests(session_id);
CREATE INDEX IF NOT EXISTS idx_interviewer_interests_interviewer ON interviewer_interests(interviewer_id);
CREATE INDEX IF NOT EXISTS idx_interviewer_interests_candidate ON interviewer_interests(candidate_id);

CREATE TABLE IF NOT EXISTS private_messages (
    id BIGSERIAL PRIMARY KEY,
    room_id VARCHAR(255) NOT NULL,
    sender_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    receiver_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    message_text TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_private_messages_room ON private_messages(room_id);
CREATE INDEX IF NOT EXISTS idx_private_messages_sender ON private_messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_private_messages_receiver ON private_messages(receiver_id);
CREATE INDEX IF NOT EXISTS idx_private_messages_receiver_unread ON private_messages(receiver_id, is_read);

CREATE TABLE IF NOT EXISTS company_shares (
    id BIGSERIAL PRIMARY KEY,
    interviewer_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    candidate_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    report_id BIGINT REFERENCES interview_reports(id) ON DELETE SET NULL,
    department VARCHAR(100),
    recipient_email VARCHAR(255),
    shared_content TEXT,
    shared_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_company_shares_interviewer ON company_shares(interviewer_id);
CREATE INDEX IF NOT EXISTS idx_company_shares_candidate ON company_shares(candidate_id);

CREATE TABLE IF NOT EXISTS rankings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL,
    score DECIMAL(5,2) NOT NULL,
    rank_position INTEGER,
    period_type VARCHAR(20) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_user_category_period UNIQUE (user_id, category, period_type, period_start)
);

CREATE INDEX IF NOT EXISTS idx_rankings_category_period ON rankings(category, period_type, period_start);
CREATE INDEX IF NOT EXISTS idx_rankings_user ON rankings(user_id);
CREATE INDEX IF NOT EXISTS idx_rankings_rank_position ON rankings(category, period_type, rank_position);

ALTER TABLE rankings DROP CONSTRAINT IF EXISTS rankings_period_type_check;
ALTER TABLE rankings ADD CONSTRAINT rankings_period_type_check CHECK (period_type IN ('WEEKLY', 'MONTHLY', 'YEARLY', 'ALL_TIME'));

CREATE TABLE IF NOT EXISTS challenges (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    challenge_type VARCHAR(50) NOT NULL,
    target_count INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reward_type VARCHAR(50),
    reward_value VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_challenges_active ON challenges(is_active);
CREATE INDEX IF NOT EXISTS idx_challenges_dates ON challenges(start_date, end_date);

ALTER TABLE challenges DROP CONSTRAINT IF EXISTS challenges_challenge_type_check;
ALTER TABLE challenges ADD CONSTRAINT challenges_challenge_type_check CHECK (challenge_type IN ('INTERVIEW_COUNT', 'CATEGORY_MASTER', 'SCORE_TARGET', 'DAILY_PRACTICE'));

CREATE TABLE IF NOT EXISTS user_challenges (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    challenge_id BIGINT REFERENCES challenges(id) ON DELETE CASCADE,
    current_count INTEGER DEFAULT 0,
    completed BOOLEAN DEFAULT FALSE,
    completed_at TIMESTAMP,
    reward_claimed BOOLEAN DEFAULT FALSE,
    CONSTRAINT unique_user_challenge UNIQUE (user_id, challenge_id)
);

CREATE INDEX IF NOT EXISTS idx_user_challenges_user ON user_challenges(user_id);
CREATE INDEX IF NOT EXISTS idx_user_challenges_completed ON user_challenges(completed);

CREATE TABLE IF NOT EXISTS achievements (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    achievement_type VARCHAR(50) NOT NULL,
    required_count INTEGER NOT NULL,
    badge_icon VARCHAR(100),
    badge_level VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_achievements_code ON achievements(code);
CREATE INDEX IF NOT EXISTS idx_achievements_type ON achievements(achievement_type);

ALTER TABLE achievements DROP CONSTRAINT IF EXISTS achievements_badge_level_check;
ALTER TABLE achievements ADD CONSTRAINT achievements_badge_level_check CHECK (badge_level IN ('BRONZE', 'SILVER', 'GOLD', 'PLATINUM', 'DIAMOND'));

INSERT INTO achievements (code, name, description, achievement_type, required_count, badge_icon, badge_level) VALUES
('FIRST_INTERVIEW', '첫 면접 완료', '첫 번째 면접을 완료했습니다', 'INTERVIEW_COUNT', 1, '🎯', 'BRONZE'),
('INTERVIEW_MASTER_10', '면접 마스터 10', '10회 면접을 완료했습니다', 'INTERVIEW_COUNT', 10, '🏆', 'SILVER'),
('INTERVIEW_MASTER_50', '면접 마스터 50', '50회 면접을 완료했습니다', 'INTERVIEW_COUNT', 50, '🏆', 'GOLD'),
('INTERVIEW_MASTER_100', '면접 마스터 100', '100회 면접을 완료했습니다', 'INTERVIEW_COUNT', 100, '🏆', 'PLATINUM'),
('PERFECT_SCORE', '완벽한 답변', 'AI 피드백 5점 만점을 받았습니다', 'SCORE_TARGET', 1, '⭐', 'GOLD'),
('BACKEND_MASTER', '백엔드 마스터', '백엔드 카테고리 상위 10%', 'CATEGORY_MASTER', 1, '💻', 'GOLD'),
('FRONTEND_MASTER', '프론트엔드 마스터', '프론트엔드 카테고리 상위 10%', 'CATEGORY_MASTER', 1, '🎨', 'GOLD')
ON CONFLICT (code) DO NOTHING;

CREATE TABLE IF NOT EXISTS user_achievements (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    achievement_id BIGINT REFERENCES achievements(id) ON DELETE CASCADE,
    achieved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_display_badge BOOLEAN DEFAULT FALSE,
    CONSTRAINT unique_user_achievement UNIQUE (user_id, achievement_id)
);

CREATE INDEX IF NOT EXISTS idx_user_achievements_user ON user_achievements(user_id);
CREATE INDEX IF NOT EXISTS idx_user_achievements_display ON user_achievements(user_id, is_display_badge);

CREATE TABLE IF NOT EXISTS system_notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content VARCHAR(1000),
    related_link VARCHAR(500),
    related_id BIGINT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_user ON system_notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread ON system_notifications(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_created ON system_notifications(created_at);

ALTER TABLE system_notifications DROP CONSTRAINT IF EXISTS system_notifications_notification_type_check;
ALTER TABLE system_notifications ADD CONSTRAINT system_notifications_notification_type_check CHECK (notification_type IN ('MESSAGE', 'INTEREST_MARKED', 'CHALLENGE_COMPLETED', 'RANKING_UPDATED', 'SHARE_NOTIFICATION', 'REPORT_COMPLETED', 'ACHIEVEMENT_UNLOCKED'));