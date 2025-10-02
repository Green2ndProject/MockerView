CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    email VARCHAR(255),
    password VARCHAR(255),
    username VARCHAR(255)
);

CREATE TABLE sessions (
    id BIGSERIAL PRIMARY KEY,
    host_id BIGINT REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PLANNED',
    session_type VARCHAR(20) CHECK (session_type IN ('GROUP','SELF')) DEFAULT 'GROUP',
    is_reviewable CHAR(1) CHECK (is_reviewable IN ('Y','N')) DEFAULT 'Y',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE questions (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT REFERENCES sessions(id),
    text TEXT NOT NULL,
    order_no INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    questioner_id BIGINT,
    timer INT
);

CREATE TABLE answers (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT REFERENCES questions(id),
    user_id BIGINT REFERENCES users(id),
    text TEXT NOT NULL,
    score INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE feedbacks (
    id BIGSERIAL PRIMARY KEY,
    answer_id BIGINT REFERENCES answers(id),
    summary TEXT,
    strengths TEXT,
    weaknesses TEXT,
    improvement TEXT,
    model VARCHAR(50) DEFAULT 'GPT-4',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewer_id BIGINT,
    score INT,
    reviewer_comment TEXT,
    feedback_type VARCHAR(20)
);

CREATE TABLE question_pool (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    difficulty VARCHAR(20),
    text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT REFERENCES sessions(id),
    reviewer_id BIGINT REFERENCES users(id),
    answer_id BIGINT REFERENCES answers(id),
    comment TEXT,
    rating DECIMAL(2,1),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);