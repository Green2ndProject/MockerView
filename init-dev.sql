CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    username VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    role VARCHAR(255) NOT NULL,
    company_name VARCHAR(255),
    company_size VARCHAR(50),
    industry VARCHAR(100),
    is_deleted INTEGER DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    withdrawal_reason VARCHAR(255)
);

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('COMPANY_ADMIN', 'COMPANY_MEMBER', 'INTERVIEWER', 'CANDIDATE', 'ADMIN'));

CREATE TABLE IF NOT EXISTS companies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    business_number VARCHAR(50) UNIQUE,
    industry VARCHAR(100),
    company_size VARCHAR(50),
    website VARCHAR(500),
    admin_user_id BIGINT REFERENCES users(id),
    subscription_plan VARCHAR(50) DEFAULT 'TRIAL',
    max_interviewers INTEGER DEFAULT 5,
    max_candidates_per_month INTEGER DEFAULT 50,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    trial_ends_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_companies_admin ON companies(admin_user_id);

CREATE TABLE IF NOT EXISTS company_members (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT REFERENCES companies(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    department VARCHAR(100),
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    CONSTRAINT unique_company_user UNIQUE (company_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_company_members_company ON company_members(company_id);
CREATE INDEX IF NOT EXISTS idx_company_members_user ON company_members(user_id);

CREATE TABLE IF NOT EXISTS sessions (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT REFERENCES companies(id) ON DELETE CASCADE,
    host_id BIGINT REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    video_recording_url VARCHAR(500),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    status VARCHAR(255),
    session_type VARCHAR(255) DEFAULT 'COMPANY_INTERVIEW',
    is_reviewable VARCHAR(255),
    agora_channel VARCHAR(255),
    media_enabled SMALLINT,
    last_activity TIMESTAMP,
    expires_at TIMESTAMP,
    difficulty VARCHAR(255),
    category VARCHAR(255),
    position VARCHAR(255),
    department VARCHAR(100),
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
CREATE INDEX IF NOT EXISTS idx_sessions_company ON sessions(company_id);

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
    company_id BIGINT REFERENCES companies(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL,
    difficulty VARCHAR(20),
    question_text TEXT NOT NULL,
    is_company_private BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_question_pool_company ON question_pool(company_id);

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
    company_id BIGINT REFERENCES companies(id) ON DELETE CASCADE,
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

CREATE INDEX IF NOT EXISTS idx_subscriptions_company ON subscriptions(company_id);

CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT REFERENCES companies(id),
    user_id BIGINT REFERENCES users(id),
    subscription_id BIGINT REFERENCES subscriptions(id),
    order_id VARCHAR(255),
    payment_key VARCHAR(255),
    transaction_id VARCHAR(255),
    method VARCHAR(20),
    amount DECIMAL(10,2),
    status VARCHAR(20),
    receipt_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS session_participants (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT REFERENCES sessions(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP,
    CONSTRAINT unique_session_user UNIQUE (session_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_session_participants_session ON session_participants(session_id);
CREATE INDEX IF NOT EXISTS idx_session_participants_user ON session_participants(user_id);

CREATE TABLE IF NOT EXISTS interviewer_notes (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT REFERENCES sessions(id) ON DELETE CASCADE,
    interviewer_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    candidate_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    strengths TEXT,
    weaknesses TEXT,
    improvement_suggestions TEXT,
    overall_impression TEXT,
    submitted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_session_interviewer UNIQUE (session_id, interviewer_id)
);

CREATE INDEX IF NOT EXISTS idx_interviewer_notes_session ON interviewer_notes(session_id);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_user_token UNIQUE (user_id, token)
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_token ON refresh_tokens(user_id, token);

CREATE TABLE IF NOT EXISTS push_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    endpoint TEXT NOT NULL,
    p256dh VARCHAR(255),
    auth VARCHAR(255),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_user_endpoint UNIQUE (user_id, endpoint)
);

CREATE INDEX IF NOT EXISTS idx_push_subscriptions_user ON push_subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_push_subscriptions_active ON push_subscriptions(user_id, active);

CREATE TABLE IF NOT EXISTS facial_analysis (
    id BIGSERIAL PRIMARY KEY,
    answer_id BIGINT REFERENCES answers(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    positive_emotion_score DOUBLE PRECISION,
    negative_emotion_score DOUBLE PRECISION,
    neutral_emotion_score DOUBLE PRECISION,
    eye_contact_frequency INTEGER,
    smile_frequency INTEGER,
    frame_count INTEGER,
    analysis_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    improvement_suggestions TEXT
);

CREATE INDEX IF NOT EXISTS idx_facial_analysis_answer ON facial_analysis(answer_id);

CREATE TABLE IF NOT EXISTS voice_analysis (
    id BIGSERIAL PRIMARY KEY,
    answer_id BIGINT REFERENCES answers(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    speech_rate DOUBLE PRECISION,
    average_pitch DOUBLE PRECISION,
    pitch_variance DOUBLE PRECISION,
    clarity_score DOUBLE PRECISION,
    pause_count INTEGER,
    average_pause_duration DOUBLE PRECISION,
    energy_level DOUBLE PRECISION,
    confidence_score DOUBLE PRECISION,
    improvement_suggestions TEXT,
    analysis_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_voice_analysis_answer ON voice_analysis(answer_id);

CREATE TABLE IF NOT EXISTS interview_mbti (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    session_id BIGINT REFERENCES sessions(id) ON DELETE CASCADE,
    analytical_score INTEGER,
    creative_score INTEGER,
    logical_score INTEGER,
    emotional_score INTEGER,
    detail_oriented_score INTEGER,
    big_picture_score INTEGER,
    decisive_score INTEGER,
    flexible_score INTEGER,
    mbti_type VARCHAR(4),
    strengths TEXT,
    weaknesses TEXT,
    career_recommendations TEXT,
    analyzed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_interview_mbti_user ON interview_mbti(user_id);

CREATE TABLE IF NOT EXISTS candidate_resumes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(1000),
    skills VARCHAR(500),
    education TEXT,
    experience TEXT,
    certifications TEXT,
    portfolio_link VARCHAR(500),
    resume_file_url VARCHAR(500),
    is_public BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_candidate_resumes_user ON candidate_resumes(user_id);
CREATE INDEX IF NOT EXISTS idx_candidate_resumes_public ON candidate_resumes(is_public);

CREATE TABLE IF NOT EXISTS talent_pool (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT REFERENCES companies(id) ON DELETE CASCADE,
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

CREATE TABLE IF NOT EXISTS interview_reports (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT REFERENCES companies(id) ON DELETE CASCADE,
    session_id BIGINT REFERENCES sessions(id) ON DELETE CASCADE,
    candidate_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    overall_score DECIMAL(5,2),
    technical_score DECIMAL(5,2),
    communication_score DECIMAL(5,2),
    cultural_fit_score DECIMAL(5,2),
    recommendation VARCHAR(50),
    detailed_feedback TEXT,
    interviewer_notes TEXT,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_interview_reports_company ON interview_reports(company_id);
CREATE INDEX IF NOT EXISTS idx_interview_reports_candidate ON interview_reports(candidate_id);

ALTER TABLE interview_reports DROP CONSTRAINT IF EXISTS interview_reports_recommendation_check;
ALTER TABLE interview_reports ADD CONSTRAINT interview_reports_recommendation_check CHECK (recommendation IN ('STRONG_HIRE', 'HIRE', 'MAYBE', 'NO_HIRE', 'STRONG_NO_HIRE'));

CREATE TABLE IF NOT EXISTS company_shares (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT REFERENCES companies(id) ON DELETE CASCADE,
    interviewer_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    candidate_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    report_id BIGINT REFERENCES interview_reports(id) ON DELETE SET NULL,
    department VARCHAR(100),
    recipient_email VARCHAR(255),
    shared_content TEXT,
    shared_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_company_shares_company ON company_shares(company_id);
CREATE INDEX IF NOT EXISTS idx_company_shares_interviewer ON company_shares(interviewer_id);
CREATE INDEX IF NOT EXISTS idx_company_shares_candidate ON company_shares(candidate_id);

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
ALTER TABLE system_notifications ADD CONSTRAINT system_notifications_notification_type_check CHECK (notification_type IN ('MESSAGE', 'INTEREST_MARKED', 'COMPANY_INVITATION', 'REPORT_COMPLETED', 'INTERVIEW_SCHEDULED', 'SHARE_NOTIFICATION'));
