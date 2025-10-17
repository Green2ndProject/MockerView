CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    username VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    role VARCHAR(255) NOT NULL,
    is_deleted INTEGER DEFAULT 0 NOT NULL,
    created_at TIMESTAMP,
    deleted_at TIMESTAMP,
    withdrawal_reason VARCHAR(255),
    CONSTRAINT users_role_check CHECK (role IN ('STUDENT', 'HOST', 'REVIEWER'))
);

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
    created_at TIMESTAMP,
    CONSTRAINT sessions_status_check CHECK (status IN ('PLANNED', 'RUNNING', 'ENDED'))
);

CREATE INDEX IF NOT EXISTS idx_sessions_expires ON sessions(expires_at);
CREATE INDEX IF NOT EXISTS idx_sessions_status_expires ON sessions(status, expires_at);

CREATE TABLE IF NOT EXISTS questions (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT REFERENCES sessions(id),
    question_text TEXT NOT NULL,
    order_no INT,
    questioner_id BIGINT REFERENCES users(id),
    timer INT,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS answers (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT REFERENCES questions(id) NOT NULL,
    user_id BIGINT REFERENCES users(id) NOT NULL,
    answer_text TEXT,
    audio_url TEXT,
    score INT,
    created_at TIMESTAMP
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
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT feedbacks_feedback_type_check CHECK (feedback_type IN ('AI', 'INTERVIEWER'))
);

CREATE TABLE IF NOT EXISTS question_pool (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    difficulty VARCHAR(20),
    question_text TEXT NOT NULL,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reviews (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT REFERENCES sessions(id),
    reviewer_id BIGINT REFERENCES users(id),
    answer_id BIGINT REFERENCES answers(id),
    review_comment TEXT,
    rating DOUBLE PRECISION,
    created_at TIMESTAMP
);

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
