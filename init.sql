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
    withdrawal_reason VARCHAR(255),
    last_login_date TIMESTAMP
);

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('STUDENT', 'HOST', 'ADMIN'));

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

CREATE TABLE IF NOT EXISTS interview_reports (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT REFERENCES sessions(id) NOT NULL,
    generated_by BIGINT REFERENCES users(id) NOT NULL,
    status VARCHAR(20) NOT NULL,
    report_content TEXT,
    summary VARCHAR(500),
    total_participants INTEGER,
    total_questions INTEGER,
    total_answers INTEGER,
    average_score DOUBLE PRECISION,
    highest_score INTEGER,
    lowest_score INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    pdf_url VARCHAR(500),
    error_message VARCHAR(1000)
);

ALTER TABLE interview_reports DROP CONSTRAINT IF EXISTS interview_reports_status_check;
ALTER TABLE interview_reports ADD CONSTRAINT interview_reports_status_check CHECK (status IN ('GENERATING', 'COMPLETED', 'FAILED'));

CREATE INDEX IF NOT EXISTS idx_interview_reports_session ON interview_reports(session_id);
CREATE INDEX IF NOT EXISTS idx_interview_reports_generated_by ON interview_reports(generated_by);
CREATE INDEX IF NOT EXISTS idx_interview_reports_status ON interview_reports(status);

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
('DEV', '개발/IT', 'IT 및 소프트웨어 개발 직군', 'MAIN', NULL, 1, TRUE, '💻'),
('DESIGN', '디자인', '시각 디자인 및 UX/UI 직군', 'MAIN', NULL, 2, TRUE, '🎨'),
('MEDICAL', '의료/보건', '의료 및 보건 관련 직군', 'MAIN', NULL, 3, TRUE, '⚕️'),
('LEGAL', '법률/회계', '법률 및 회계 전문 직군', 'MAIN', NULL, 4, TRUE, '⚖️'),
('EDUCATION', '교육', '교육 및 강사 직군', 'MAIN', NULL, 5, TRUE, '📚'),
('FINANCE', '금융/보험', '금융 및 보험 관련 직군', 'MAIN', NULL, 6, TRUE, '💰'),
('BUSINESS', '경영/기획', '경영 및 사업 기획 직군', 'MAIN', NULL, 7, TRUE, '📊'),
('MARKETING', '마케팅', '마케팅 및 광고 직군', 'MAIN', NULL, 8, TRUE, '📢'),
('SALES', '영업', '영업 및 고객 관리 직군', 'MAIN', NULL, 9, TRUE, '🤝'),
('SERVICE', '서비스', '서비스 및 접객 직군', 'MAIN', NULL, 10, TRUE, '💁'),
('PUBLIC', '공공/행정', '공무원 및 공공기관 직군', 'MAIN', NULL, 11, TRUE, '🏛️'),
('MANUFACTURE', '제조/생산', '제조 및 생산관리 직군', 'MAIN', NULL, 12, TRUE, '🏭'),
('LOGISTICS', '물류/유통', '물류 및 유통 관리 직군', 'MAIN', NULL, 13, TRUE, '🚚'),
('CONSTRUCTION', '건설/건축', '건설 및 건축 관련 직군', 'MAIN', NULL, 14, TRUE, '🏗️'),
('MEDIA', '미디어/콘텐츠', '미디어 제작 및 콘텐츠 직군', 'MAIN', NULL, 15, TRUE, '🎬'),
('RESEARCH', '연구/R&D', '연구개발 및 실험 직군', 'MAIN', NULL, 16, TRUE, '🔬'),
('HR', '인사/총무', '인사 및 총무 관리 직군', 'MAIN', NULL, 17, TRUE, '👥')
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active,
    icon = EXCLUDED.icon;

INSERT INTO categories (code, name, description, category_type, parent_id, display_order, is_active, icon) VALUES
('DEV_FRONTEND', '프론트엔드 개발자', 'React, Vue, Angular 등', 'SUB', (SELECT id FROM categories WHERE code='DEV'), 1, TRUE, '🌐'),
('DEV_BACKEND', '백엔드 개발자', 'Java, Spring, Node.js 등', 'SUB', (SELECT id FROM categories WHERE code='DEV'), 2, TRUE, '⚙️'),
('DEV_FULLSTACK', '풀스택 개발자', '프론트+백엔드 통합', 'SUB', (SELECT id FROM categories WHERE code='DEV'), 3, TRUE, '🔄'),
('DEV_MOBILE', '모바일 개발자', 'iOS, Android, Flutter', 'SUB', (SELECT id FROM categories WHERE code='DEV'), 4, TRUE, '📱'),
('DEV_GAME', '게임 개발자', 'Unity, Unreal Engine', 'SUB', (SELECT id FROM categories WHERE code='DEV'), 5, TRUE, '🎮'),
('DEV_AI', 'AI/ML 엔지니어', '머신러닝, 딥러닝', 'SUB', (SELECT id FROM categories WHERE code='DEV'), 6, TRUE, '🤖'),
('DEV_DATA', '데이터 엔지니어', '데이터 파이프라인, ETL', 'SUB', (SELECT id FROM categories WHERE code='DEV'), 7, TRUE, '🗄️'),
('DEV_DEVOPS', '데브옵스 엔지니어', 'CI/CD, Docker, K8s', 'SUB', (SELECT id FROM categories WHERE code='DEV'), 8, TRUE, '☁️'),
('DEV_SECURITY', '보안 엔지니어', '정보보안, 해킹방어', 'SUB', (SELECT id FROM categories WHERE code='DEV'), 9, TRUE, '🔐'),
('DEV_QA', 'QA/테스터', '품질관리, 테스트자동화', 'SUB', (SELECT id FROM categories WHERE code='DEV'), 10, TRUE, '✅'),
('DESIGN_UIUX', 'UI/UX 디자이너', '사용자 경험 설계', 'SUB', (SELECT id FROM categories WHERE code='DESIGN'), 1, TRUE, '🎯'),
('DESIGN_GRAPHIC', '그래픽 디자이너', '시각 디자인, 브랜딩', 'SUB', (SELECT id FROM categories WHERE code='DESIGN'), 2, TRUE, '🖌️'),
('DESIGN_WEB', '웹 디자이너', '웹사이트 디자인', 'SUB', (SELECT id FROM categories WHERE code='DESIGN'), 3, TRUE, '🌐'),
('DESIGN_PRODUCT', '제품 디자이너', '제품 기획 및 디자인', 'SUB', (SELECT id FROM categories WHERE code='DESIGN'), 4, TRUE, '📦'),
('DESIGN_VIDEO', '영상 디자이너', '모션그래픽, 편집', 'SUB', (SELECT id FROM categories WHERE code='DESIGN'), 5, TRUE, '🎬'),
('MEDICAL_DOCTOR', '의사', '진료 및 치료', 'SUB', (SELECT id FROM categories WHERE code='MEDICAL'), 1, TRUE, '🩺'),
('MEDICAL_NURSE', '간호사', '간호 및 환자 케어', 'SUB', (SELECT id FROM categories WHERE code='MEDICAL'), 2, TRUE, '💉'),
('MEDICAL_PHARMACIST', '약사', '조제 및 복약지도', 'SUB', (SELECT id FROM categories WHERE code='MEDICAL'), 3, TRUE, '💊'),
('MEDICAL_DENTAL', '치과의사', '치과 진료', 'SUB', (SELECT id FROM categories WHERE code='MEDICAL'), 4, TRUE, '🦷'),
('MEDICAL_RADIOLOGIST', '방사선사', '영상의학', 'SUB', (SELECT id FROM categories WHERE code='MEDICAL'), 5, TRUE, '📡'),
('MEDICAL_THERAPIST', '물리치료사', '재활 및 물리치료', 'SUB', (SELECT id FROM categories WHERE code='MEDICAL'), 6, TRUE, '🏃'),
('MEDICAL_NUTRITION', '영양사', '영양 상담 및 관리', 'SUB', (SELECT id FROM categories WHERE code='MEDICAL'), 7, TRUE, '🥗'),
('LEGAL_LAWYER', '변호사', '법률 자문 및 소송', 'SUB', (SELECT id FROM categories WHERE code='LEGAL'), 1, TRUE, '⚖️'),
('LEGAL_ACCOUNTANT', '회계사', '재무제표, 회계감사', 'SUB', (SELECT id FROM categories WHERE code='LEGAL'), 2, TRUE, '💼'),
('LEGAL_TAX', '세무사', '세무신고 및 자문', 'SUB', (SELECT id FROM categories WHERE code='LEGAL'), 3, TRUE, '📝'),
('LEGAL_PATENT', '변리사', '특허 및 지적재산권', 'SUB', (SELECT id FROM categories WHERE code='LEGAL'), 4, TRUE, '📄'),
('EDU_TEACHER', '교사', '초중고 교육', 'SUB', (SELECT id FROM categories WHERE code='EDUCATION'), 1, TRUE, '👨‍🏫'),
('EDU_PROFESSOR', '교수', '대학 강의 및 연구', 'SUB', (SELECT id FROM categories WHERE code='EDUCATION'), 2, TRUE, '🎓'),
('EDU_INSTRUCTOR', '강사', '학원 및 직업교육', 'SUB', (SELECT id FROM categories WHERE code='EDUCATION'), 3, TRUE, '📖'),
('EDU_COUNSELOR', '상담사', '진로 및 심리상담', 'SUB', (SELECT id FROM categories WHERE code='EDUCATION'), 4, TRUE, '💬'),
('FIN_BANKER', '은행원', '여신, 자산관리', 'SUB', (SELECT id FROM categories WHERE code='FINANCE'), 1, TRUE, '🏦'),
('FIN_INSURANCE', '보험설계사', '보험상품 판매', 'SUB', (SELECT id FROM categories WHERE code='FINANCE'), 2, TRUE, '🛡️'),
('FIN_SECURITIES', '증권 애널리스트', '주식 분석 및 투자', 'SUB', (SELECT id FROM categories WHERE code='FINANCE'), 3, TRUE, '📈'),
('FIN_WEALTH', '자산관리사', 'PB, 재무설계', 'SUB', (SELECT id FROM categories WHERE code='FINANCE'), 4, TRUE, '💎'),
('BIZ_STRATEGY', '경영기획', '사업전략 수립', 'SUB', (SELECT id FROM categories WHERE code='BUSINESS'), 1, TRUE, '♟️'),
('BIZ_CONSULTING', '컨설턴트', '경영 자문', 'SUB', (SELECT id FROM categories WHERE code='BUSINESS'), 2, TRUE, '💡'),
('BIZ_PM', 'PM/PO', '프로젝트/프로덕트 관리', 'SUB', (SELECT id FROM categories WHERE code='BUSINESS'), 3, TRUE, '📋'),
('MKT_DIGITAL', '디지털 마케터', '퍼포먼스 마케팅', 'SUB', (SELECT id FROM categories WHERE code='MARKETING'), 1, TRUE, '📱'),
('MKT_BRAND', '브랜드 마케터', '브랜딩, 커뮤니케이션', 'SUB', (SELECT id FROM categories WHERE code='MARKETING'), 2, TRUE, '✨'),
('MKT_CONTENT', '콘텐츠 마케터', '콘텐츠 기획 제작', 'SUB', (SELECT id FROM categories WHERE code='MARKETING'), 3, TRUE, '✍️'),
('MKT_SNS', 'SNS 마케터', '소셜미디어 운영', 'SUB', (SELECT id FROM categories WHERE code='MARKETING'), 4, TRUE, '📲'),
('SALES_B2B', 'B2B 영업', '기업 대상 영업', 'SUB', (SELECT id FROM categories WHERE code='SALES'), 1, TRUE, '🏢'),
('SALES_B2C', 'B2C 영업', '소비자 대상 영업', 'SUB', (SELECT id FROM categories WHERE code='SALES'), 2, TRUE, '🛍️'),
('SALES_RETAIL', '리테일 영업', '유통 매장 관리', 'SUB', (SELECT id FROM categories WHERE code='SALES'), 3, TRUE, '🏬'),
('SVC_CS', '고객상담', 'CS, 콜센터', 'SUB', (SELECT id FROM categories WHERE code='SERVICE'), 1, TRUE, '☎️'),
('SVC_HOTEL', '호텔리어', '호텔 서비스', 'SUB', (SELECT id FROM categories WHERE code='SERVICE'), 2, TRUE, '🏨'),
('SVC_FLIGHT', '승무원', '항공 서비스', 'SUB', (SELECT id FROM categories WHERE code='SERVICE'), 3, TRUE, '✈️'),
('SVC_FOOD', '외식업', '요식업 관리', 'SUB', (SELECT id FROM categories WHERE code='SERVICE'), 4, TRUE, '🍽️'),
('PUB_ADMIN', '일반행정직', '행정업무 전반', 'SUB', (SELECT id FROM categories WHERE code='PUBLIC'), 1, TRUE, '🏛️'),
('PUB_POLICE', '경찰/소방', '치안 및 소방', 'SUB', (SELECT id FROM categories WHERE code='PUBLIC'), 2, TRUE, '👮'),
('PUB_SOCIAL', '사회복지사', '복지 상담 지원', 'SUB', (SELECT id FROM categories WHERE code='PUBLIC'), 3, TRUE, '🤲'),
('MFG_PRODUCTION', '생산관리', '공정 관리', 'SUB', (SELECT id FROM categories WHERE code='MANUFACTURE'), 1, TRUE, '⚙️'),
('MFG_QC', '품질관리', 'QC, 검사', 'SUB', (SELECT id FROM categories WHERE code='MANUFACTURE'), 2, TRUE, '✅'),
('MFG_ENGINEER', '설비 엔지니어', '기계/전기 설비', 'SUB', (SELECT id FROM categories WHERE code='MANUFACTURE'), 3, TRUE, '🔧'),
('LOG_SCM', 'SCM 관리', '공급망 관리', 'SUB', (SELECT id FROM categories WHERE code='LOGISTICS'), 1, TRUE, '📦'),
('LOG_WAREHOUSE', '물류센터 관리', '창고 운영', 'SUB', (SELECT id FROM categories WHERE code='LOGISTICS'), 2, TRUE, '🏭'),
('LOG_PURCHASE', '구매/자재', '구매 및 자재관리', 'SUB', (SELECT id FROM categories WHERE code='LOGISTICS'), 3, TRUE, '🛒'),
('CON_ARCHITECT', '건축가', '건축 설계', 'SUB', (SELECT id FROM categories WHERE code='CONSTRUCTION'), 1, TRUE, '🏗️'),
('CON_CIVIL', '토목 엔지니어', '토목 설계 시공', 'SUB', (SELECT id FROM categories WHERE code='CONSTRUCTION'), 2, TRUE, '🛤️'),
('CON_INTERIOR', '인테리어 디자이너', '실내 설계', 'SUB', (SELECT id FROM categories WHERE code='CONSTRUCTION'), 3, TRUE, '🛋️'),
('MEDIA_PD', 'PD/프로듀서', '콘텐츠 제작', 'SUB', (SELECT id FROM categories WHERE code='MEDIA'), 1, TRUE, '🎬'),
('MEDIA_WRITER', '작가/시나리오', '대본 및 기획', 'SUB', (SELECT id FROM categories WHERE code='MEDIA'), 2, TRUE, '✍️'),
('MEDIA_EDITOR', '영상 편집자', '영상 후반작업', 'SUB', (SELECT id FROM categories WHERE code='MEDIA'), 3, TRUE, '🎞️'),
('MEDIA_REPORTER', '기자', '취재 및 기사작성', 'SUB', (SELECT id FROM categories WHERE code='MEDIA'), 4, TRUE, '📰'),
('RND_SCIENTIST', '연구원', '기초/응용 연구', 'SUB', (SELECT id FROM categories WHERE code='RESEARCH'), 1, TRUE, '🔬'),
('RND_LAB', '실험실 기술자', '실험 수행 지원', 'SUB', (SELECT id FROM categories WHERE code='RESEARCH'), 2, TRUE, '🧪'),
('RND_DATA', '데이터 사이언티스트', '데이터 분석 연구', 'SUB', (SELECT id FROM categories WHERE code='RESEARCH'), 3, TRUE, '📊'),
('HR_RECRUIT', '채용 담당', '인재 채용', 'SUB', (SELECT id FROM categories WHERE code='HR'), 1, TRUE, '🎯'),
('HR_TRAINING', '교육 담당', '직원 교육 개발', 'SUB', (SELECT id FROM categories WHERE code='HR'), 2, TRUE, '📚'),
('HR_PAYROLL', '급여 담당', '급여 및 복리후생', 'SUB', (SELECT id FROM categories WHERE code='HR'), 3, TRUE, '💵'),
('HR_GENERAL', '총무', '시설 및 자산관리', 'SUB', (SELECT id FROM categories WHERE code='HR'), 4, TRUE, '🏢')
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    parent_id = EXCLUDED.parent_id,
    display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active,
    icon = EXCLUDED.icon;

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
    amount DECIMAL(10,2),
    status VARCHAR(20),
    receipt_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    -- user_id BIGINT REFERENCES users(id) NOT NULL,
    token VARCHAR(512) UNIQUE NOT NULL,
    -- expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens(token);

CREATE TABLE IF NOT EXISTS push_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    endpoint TEXT NOT NULL,
    p256dh_key VARCHAR(255) NOT NULL,
    auth_key VARCHAR(255) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_push_subscriptions_user ON push_subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_push_subscriptions_active ON push_subscriptions(active);

CREATE TABLE IF NOT EXISTS session_participants (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT REFERENCES sessions(id),
    user_id BIGINT REFERENCES users(id),
    role VARCHAR(50),
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS interviewer_notes (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT REFERENCES sessions(id) UNIQUE,
    interviewer_id BIGINT REFERENCES users(id),
    strengths TEXT,
    weaknesses TEXT,
    improvements TEXT,
    submitted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS voice_analyses (
    id BIGSERIAL PRIMARY KEY,
    answer_id BIGINT REFERENCES answers(id) UNIQUE,
    speed_wpm DOUBLE PRECISION,
    tone_average DOUBLE PRECISION,
    tone_variance DOUBLE PRECISION,
    clarity_score DOUBLE PRECISION,
    pause_count INTEGER,
    pause_avg_length DOUBLE PRECISION,
    energy_level DOUBLE PRECISION,
    confidence_score DOUBLE PRECISION,
    suggestions TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS facial_analyses (
    id BIGSERIAL PRIMARY KEY,
    answer_id BIGINT REFERENCES answers(id) UNIQUE,
    positive_score DOUBLE PRECISION,
    negative_score DOUBLE PRECISION,
    neutral_score DOUBLE PRECISION,
    eye_contact_score DOUBLE PRECISION,
    smile_frequency INTEGER,
    frame_count INTEGER,
    timestamps TEXT,
    suggestions TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS interview_mbti (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) UNIQUE,
    session_id BIGINT REFERENCES sessions(id),
    mbti_type VARCHAR(4),
    analytical_score INTEGER,
    creative_score INTEGER,
    logical_score INTEGER,
    emotional_score INTEGER,
    detail_oriented_score INTEGER,
    big_picture_score INTEGER,
    decisive_score INTEGER,
    flexible_score INTEGER,
    strengths TEXT,
    weaknesses TEXT,
    career_recommendations TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

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

CREATE TABLE self_interview_reports (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    category_code VARCHAR(100) NOT NULL,
    category_name VARCHAR(255) NOT NULL,
    question_type VARCHAR(50) NOT NULL,
    difficulty INTEGER NOT NULL,
    total_questions INTEGER NOT NULL,
    overall_avg DOUBLE PRECISION,
    text_avg DOUBLE PRECISION,
    audio_avg DOUBLE PRECISION,
    video_avg DOUBLE PRECISION,
    questions_data TEXT,
    feedbacks_data TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_self_interview_reports_user_id ON self_interview_reports(user_id);
CREATE INDEX idx_self_interview_reports_created_at ON self_interview_reports(created_at);

CREATE INDEX IF NOT EXISTS idx_notifications_user ON system_notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread ON system_notifications(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_created ON system_notifications(created_at);

ALTER TABLE system_notifications DROP CONSTRAINT IF EXISTS system_notifications_notification_type_check;
ALTER TABLE system_notifications ADD CONSTRAINT system_notifications_notification_type_check CHECK (notification_type IN ('MESSAGE', 'INTEREST_MARKED', 'CHALLENGE_COMPLETED', 'RANKING_UPDATED', 'SHARE_NOTIFICATION', 'REPORT_COMPLETED', 'ACHIEVEMENT_UNLOCKED'));

ALTER TABLE users ADD COLUMN IF NOT EXISTS agree_personal_info BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS agree_third_party BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS agree_marketing BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS agree_marketing_email BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS agree_marketing_push BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS privacy_consent_date TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_date TIMESTAMP;

COMMENT ON COLUMN users.agree_personal_info IS '개인정보 수집 및 이용 동의';
COMMENT ON COLUMN users.agree_third_party IS '개인정보 제3자 제공 동의';
COMMENT ON COLUMN users.agree_marketing IS '마케팅 정보 수신 동의';
COMMENT ON COLUMN users.agree_marketing_email IS '마케팅 이메일 수신 동의';
COMMENT ON COLUMN users.agree_marketing_push IS '마케팅 웹푸시 수신 동의';
COMMENT ON COLUMN users.privacy_consent_date IS '개인정보 동의 일시';
COMMENT ON COLUMN users.last_login_date IS '마지막 로그인 일시';