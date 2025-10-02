let currentQuestionIndex = 0;
let questions = [];
let answers = [];
let feedbacks = [];
let sessionStartTime = Date.now();
let questionStartTime = Date.now();
let sessionTimer;
let questionTimer;
let SESSION_DATA = null;

document.addEventListener('DOMContentLoaded', async function() {
    try {
        const pathParts = window.location.pathname.split('/');
        const sessionId = pathParts[pathParts.length - 1];
        
        console.log('Loading session:', sessionId);
        
        const response = await authFetch(`/api/selfinterview/${sessionId}`);
        if (!response.ok) {
            throw new Error('세션을 불러올 수 없습니다');
        }
        
        SESSION_DATA = await response.json();
        questions = SESSION_DATA.questions || [];
        
        console.log('SESSION_DATA loaded:', SESSION_DATA);
        console.log('Questions:', questions);
        
        initializeSession();
        startSessionTimer();
    } catch (error) {
        console.error('Failed to load session:', error);
        alert('세션을 불러오는 중 오류가 발생했습니다: ' + error.message);
    }
});

function initializeSession() {
    if (questions.length === 0) {
        alert('질문을 불러올 수 없습니다.');
        return;
    }

    document.getElementById('progress-total').textContent = questions.length;
    renderQuestionList();
    loadQuestion(0);
    setupTextareaCounter();
}

function setupTextareaCounter() {
    const answerTextArea = document.getElementById('answerText');
    if (answerTextArea) {
        answerTextArea.addEventListener('input', function() {
            const count = this.value.length;
            document.getElementById('charCount').textContent = count;
            if (count > 1000) {
                this.value = this.value.substring(0, 1000);
                document.getElementById('charCount').textContent = 1000;
            }
        });
    }
}

function startSessionTimer() {
    sessionTimer = setInterval(() => {
        const elapsed = Math.floor((Date.now() - sessionStartTime) / 1000);
        const minutes = Math.floor(elapsed / 60);
        const seconds = elapsed % 60;
        document.getElementById('session-timer').textContent = 
            `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
    }, 1000);
}

function startQuestionTimer() {
    if (questionTimer) clearInterval(questionTimer);
    questionStartTime = Date.now();
    questionTimer = setInterval(() => {
        const elapsed = Math.floor((Date.now() - questionStartTime) / 1000);
        const minutes = Math.floor(elapsed / 60);
        const seconds = elapsed % 60;
        document.getElementById('question-timer').textContent = 
            `⏱️ ${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
    }, 1000);
}

function loadQuestion(index) {
    if (index >= questions.length) {
        completeInterview();
        return;
    }

    currentQuestionIndex = index;
    const question = questions[index];

    document.getElementById('question-number').textContent = `Q${index + 1}`;
    document.getElementById('current-question-text').textContent = question.questionText;
    document.getElementById('answerText').value = '';
    document.getElementById('charCount').textContent = '0';

    updateProgress();
    updateQuestionListUI();
    startQuestionTimer();
}

function updateProgress() {
    const answered = answers.length;
    const progress = (answered / questions.length) * 100;
    document.getElementById('progress-fill').style.width = `${progress}%`;
    document.getElementById('progress-current').textContent = answered;
}

function renderQuestionList() {
    const questionList = document.getElementById('question-list');
    questionList.innerHTML = '';

    questions.forEach((q, index) => {
        const li = document.createElement('li');
        li.className = 'question-nav-item';
        li.textContent = `Q${index + 1}: ${q.questionText.substring(0, 30)}...`;
        li.onclick = () => loadQuestion(index);
        questionList.appendChild(li);
    });
}

function updateQuestionListUI() {
    const items = document.querySelectorAll('.question-nav-item');
    items.forEach((item, index) => {
        item.classList.remove('active', 'completed');
        if (index === currentQuestionIndex) {
            item.classList.add('active');
        } else if (index < currentQuestionIndex) {
            item.classList.add('completed');
        }
    });
}

async function submitAnswer() {
    const answerText = document.getElementById('answerText').value.trim();
    
    if (!answerText) {
        alert('답변을 입력해주세요.');
        return;
    }

    if (!SESSION_DATA || !SESSION_DATA.sessionId) {
        alert('세션 정보를 불러올 수 없습니다.');
        return;
    }

    const question = questions[currentQuestionIndex];
    const answerData = {
        questionId: question.id,
        answerText: answerText
    };

    console.log('Submitting answer:', answerData);
    console.log('Session ID:', SESSION_DATA.sessionId);

    try {
        document.getElementById('ai-status').textContent = '분석중...';
        document.getElementById('ai-status').classList.add('analyzing');

        const response = await authFetch(`/api/selfinterview/${SESSION_DATA.sessionId}/answer`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(answerData)
        });

        console.log('Response status:', response.status);

        if (!response.ok) {
            const errorText = await response.text();
            console.error('Error response:', errorText);
            throw new Error('답변 제출 실패');
        }

        const result = await response.json();
        console.log('Result:', result);
        
        answers.push(result.answer);
        feedbacks.push(result.feedback);

        displayFeedback(result.feedback, question);

        document.getElementById('ai-status').textContent = '완료';
        document.getElementById('ai-status').classList.remove('analyzing');
        document.getElementById('ai-status').classList.add('completed');

        updateProgress();

        setTimeout(() => {
            loadQuestion(currentQuestionIndex + 1);
        }, 1500);

    } catch (error) {
        console.error('답변 제출 오류:', error);
        alert('답변 제출 중 오류가 발생했습니다: ' + error.message);
        document.getElementById('ai-status').textContent = '오류';
        document.getElementById('ai-status').classList.remove('analyzing');
    }
}

function displayFeedback(feedback, question) {
    const feedbackList = document.getElementById('ai-feedback-list');
    
    if (feedbackList.querySelector('.empty-state')) {
        feedbackList.innerHTML = '';
    }

    const feedbackItem = document.createElement('div');
    feedbackItem.className = 'feedback-item';
    feedbackItem.innerHTML = `
        <div class="feedback-question">Q${currentQuestionIndex + 1}: ${question.questionText.substring(0, 40)}...</div>
        <div class="feedback-score">
            <span class="score-badge">${feedback.score}점</span>
            <span>${getScoreLabel(feedback.score)}</span>
        </div>
        <div class="feedback-content">
            <div class="feedback-section">
                <h5>💪 강점</h5>
                <p>${feedback.strengths || '분석 중...'}</p>
            </div>
            <div class="feedback-section">
                <h5>📈 개선점</h5>
                <p>${feedback.improvements || '분석 중...'}</p>
            </div>
        </div>
    `;

    feedbackList.insertBefore(feedbackItem, feedbackList.firstChild);
}

function getScoreLabel(score) {
    if (score >= 90) return '탁월함';
    if (score >= 80) return '우수함';
    if (score >= 70) return '양호함';
    if (score >= 60) return '보통';
    return '개선 필요';
}

function skipQuestion() {
    if (confirm('이 질문을 건너뛰시겠습니까?')) {
        loadQuestion(currentQuestionIndex + 1);
    }
}

function completeInterview() {
    clearInterval(sessionTimer);
    clearInterval(questionTimer);

    const totalTime = Math.floor((Date.now() - sessionStartTime) / 60000);
    const avgScore = feedbacks.length > 0 
        ? Math.round(feedbacks.reduce((sum, f) => sum + f.score, 0) / feedbacks.length) 
        : 0;

    document.getElementById('answer-section').style.display = 'none';
    document.querySelector('.current-question').style.display = 'none';
    document.getElementById('result-section').style.display = 'block';

    document.getElementById('avg-score').textContent = `${avgScore}점`;
    document.getElementById('answered-count').textContent = `${answers.length}개`;
    document.getElementById('total-time').textContent = `${totalTime}분`;
}

function toggleResultView() {
    completeInterview();
}

function viewDetailedResults() {
    if (SESSION_DATA && SESSION_DATA.sessionId) {
        window.location.href = `/session/detail/${SESSION_DATA.sessionId}`;
    }
}
