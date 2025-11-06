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
    score INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ai_feedback_requested BOOLEAN DEFAULT TRUE,
    ai_feedback_generated BOOLEAN DEFAULT FALSE,
    ai_feedback_skipped_reason VARCHAR(255),
    ai_processing_time_ms BIGINT
);

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
        INSERT INTO subscriptions (user_id, plan_type, status, start_date, end_date, next_billing_date, session_limit, used_sessions, auto_renew, created_at, updated_at)
        SELECT 
            u.id,
            'FREE',
            'ACTIVE',
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP + INTERVAL '1 month',
            CURRENT_TIMESTAMP + INTERVAL '1 month',
            5,
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

CREATE TABLE IF NOT EXISTS session_participants (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    is_online BOOLEAN DEFAULT FALSE,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP,
    UNIQUE(session_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_session_participants_session ON session_participants(session_id);
CREATE INDEX IF NOT EXISTS idx_session_participants_online ON session_participants(session_id, is_online);