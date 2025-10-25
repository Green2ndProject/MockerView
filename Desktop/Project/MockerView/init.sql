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