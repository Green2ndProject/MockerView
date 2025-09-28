CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20) CHECK (role IN ('STUDENT','HOST','REVIEWER')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sessions (
    id BIGSERIAL PRIMARY KEY,
    host_id BIGINT REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    status VARCHAR(20) CHECK (status IN ('PLANNED','RUNNING','ENDED')) DEFAULT 'PLANNED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE questions (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT REFERENCES sessions(id),
    text TEXT NOT NULL,
    order_no INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE answers (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT REFERENCES questions(id),
    user_id BIGINT REFERENCES users(id),
    text TEXT NOT NULL,
    score INT CHECK (score BETWEEN 1 AND 10),
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
