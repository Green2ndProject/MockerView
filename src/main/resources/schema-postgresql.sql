CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    email VARCHAR(255),
    password VARCHAR(255),
    username VARCHAR(255),
    is_deleted SMALLINT DEFAULT 0 NOT NULL,
    deleted_at TIMESTAMP,
    withdrawal_reason VARCHAR(255),
    CONSTRAINT chk_is_deleted CHECK (is_deleted IN (0, 1))
);

CREATE TABLE sessions (
    id BIGSERIAL PRIMARY KEY,
    host_id BIGINT REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PLANNED',
    session_type VARCHAR(20) DEFAULT 'GROUP',
    is_reviewable CHAR(1) DEFAULT 'Y',
    is_self_interview CHAR(1) DEFAULT 'N',
    agora_channel VARCHAR(255),
    media_enabled SMALLINT DEFAULT 0,
    last_activity TIMESTAMP,
    expires_at TIMESTAMP,
    difficulty VARCHAR(20),
    category VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_status CHECK (status IN ('PLANNED','RUNNING','ENDED')),
    CONSTRAINT chk_session_type CHECK (session_type IN ('GROUP','SELF','TEXT','AUDIO','VIDEO')),
    CONSTRAINT chk_is_reviewable CHECK (is_reviewable IN ('Y','N')),
    CONSTRAINT chk_media_enabled CHECK (media_enabled IN (0,1))
);

CREATE INDEX idx_sessions_expires ON sessions(expires_at);
CREATE INDEX idx_sessions_status_expires ON sessions(status, expires_at);

CREATE TABLE questions (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT REFERENCES sessions(id),
    question_text TEXT NOT NULL,
    order_no INT DEFAULT 1,
    questioner_id BIGINT,
    timer INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE answers (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT REFERENCES questions(id),
    user_id BIGINT REFERENCES users(id),
    answer_text TEXT NOT NULL,
    score INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_score CHECK (score BETWEEN 1 AND 100)
);

CREATE TABLE feedbacks (
    id BIGSERIAL PRIMARY KEY,
    answer_id BIGINT REFERENCES answers(id),
    summary TEXT,
    strengths TEXT,
    weaknesses TEXT,
    improvement TEXT,
    model VARCHAR(50) DEFAULT 'GPT-4',
    feedback_type VARCHAR(20),
    reviewer_id BIGINT,
    reviewer_comment TEXT,
    score INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_feedback_type CHECK (feedback_type IN ('AI','INTERVIEWER'))
);

CREATE TABLE question_pool (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    difficulty VARCHAR(20),
    question_text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_difficulty CHECK (difficulty IN ('EASY','MEDIUM','HARD'))
);

CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT REFERENCES sessions(id),
    reviewer_id BIGINT REFERENCES users(id),
    answer_id BIGINT REFERENCES answers(id),
    review_comment TEXT,
    rating DECIMAL(2,1),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_rating CHECK (rating BETWEEN 0.0 AND 5.0)
);

INSERT INTO question_pool (category, difficulty, question_text) VALUES ('기술', 'EASY', 'Java의 JVM 구조에 대해 설명해주세요.');
INSERT INTO question_pool (category, difficulty, question_text) VALUES ('기술', 'MEDIUM', 'Spring과 Spring Boot의 차이점은 무엇인가요?');
INSERT INTO question_pool (category, difficulty, question_text) VALUES ('기술', 'HARD', 'RESTful API 설계 원칙과 실제 프로젝트 적용 경험을 말씀해주세요.');
INSERT INTO question_pool (category, difficulty, question_text) VALUES ('인성', 'EASY', '자신의 장점과 단점을 말씀해주세요.');
INSERT INTO question_pool (category, difficulty, question_text) VALUES ('인성', 'MEDIUM', '팀 프로젝트에서 갈등을 해결한 경험이 있나요?');
INSERT INTO question_pool (category, difficulty, question_text) VALUES ('상황', 'MEDIUM', '마감 기한이 촉박한 상황에서 어떻게 대처하시나요?');
INSERT INTO question_pool (category, difficulty, question_text) VALUES ('상황', 'HARD', '기술적 의견 충돌이 있을 때 어떻게 해결하시나요?');