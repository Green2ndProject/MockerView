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
        
        if (questions.length === 0) {
            alert('질문을 불러올 수 없습니다.');
            return;
        }
        
        initializeSession();
        startSessionTimer();
    } catch (error) {
        console.error('Failed to load session:', error);
        alert('세션을 불러오는 중 오류가 발생했습니다: ' + error.message);
    }
});

function initializeSession() {
    document.getElementById('progress-total').textContent = questions.length;
    document.getElementById('session-stats').textContent = `총 ${questions.length}개 질문`;
    renderQuestionList();
    loadQuestion(0);
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
            `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
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
    
    const answerTextArea = document.getElementById('answerText');
    if (answerTextArea) {
        answerTextArea.value = '';
    }
    
    const charCount = document.getElementById('charCount');
    if (charCount) {
        charCount.textContent = '0';
    }

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
        li.innerHTML = `<strong>Q${index + 1}</strong> ${q.questionText.substring(0, 30)}...`;
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

window.submitTextAnswer = async function() {
    const answerText = document.getElementById('answerText').value.trim();
    
    if (!answerText) {
        alert('답변을 입력해주세요.');
        return;
    }

    if (!SESSION_DATA || !SESSION_DATA.sessionId) {
        alert('세션 정보를 불러올 수 없습니다.');
        console.error('SESSION_DATA:', SESSION_DATA);
        return;
    }

    const question = questions[currentQuestionIndex];
    const answerData = {
        questionId: question.id,
        answerText: answerText
    };

    console.log('Submitting answer:', answerData);
    console.log('To endpoint:', `/api/selfinterview/${SESSION_DATA.sessionId}/answer`);

    try {
        document.getElementById('ai-status').textContent = '분석중...';
        document.getElementById('ai-status').style.color = '#f59e0b';

        const response = await authFetch(`/api/selfinterview/${SESSION_DATA.sessionId}/answer`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(answerData)
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error('답변 제출 실패: ' + errorText);
        }

        const result = await response.json();
        console.log('Answer result:', result);
        console.log('Current question:', question);
        
        if (!result.answer || !result.feedback) {
            throw new Error('Invalid response structure');
        }
        
        answers.push(result.answer);
        feedbacks.push(result.feedback);

        displayFeedback(result.feedback, question, currentQuestionIndex + 1);

        document.getElementById('ai-status').textContent = '완료';
        document.getElementById('ai-status').style.color = '#10b981';
        updateProgress();

        setTimeout(() => {
            loadQuestion(currentQuestionIndex + 1);
        }, 1500);

    } catch (error) {
        console.error('답변 제출 오류:', error);
        alert('답변 제출 중 오류가 발생했습니다: ' + error.message);
        document.getElementById('ai-status').textContent = '오류';
        document.getElementById('ai-status').style.color = '#ef4444';
    }
};

window.submitVoiceAnswerSelf = async function(audioBlob) {
    if (!SESSION_DATA || !SESSION_DATA.sessionId) {
        alert('세션 정보를 불러올 수 없습니다.');
        return;
    }

    const question = questions[currentQuestionIndex];
    const formData = new FormData();
    formData.append('audio', audioBlob, 'answer.webm');
    formData.append('questionId', question.id);

    try {
        document.getElementById('ai-status').textContent = '음성 변환 중...';
        document.getElementById('ai-status').style.color = '#f59e0b';
        
        const token = document.cookie.split(';').find(c => c.trim().startsWith('Authorization='));
        const authToken = token ? token.split('=')[1] : '';
        
        const response = await fetch(`/api/session/${SESSION_DATA.sessionId}/voice-answer`, {
            method: 'POST',
            headers: {
                'Authorization': authToken
            },
            body: formData
        });
        
        if (!response.ok) {
            throw new Error('음성 변환 실패: ' + response.status);
        }
        
        const result = await response.json();
        console.log('Voice answer result:', result);
        
        answers.push(result.answer);
        feedbacks.push(result.feedback);
        
        displayFeedback(result.feedback, question, currentQuestionIndex + 1);
        
        document.getElementById('ai-status').textContent = '완료';
        document.getElementById('ai-status').style.color = '#10b981';
        
        window.audioBlob = null;
        
        const recordingStatus = document.getElementById('recordingStatus');
        if (recordingStatus) {
            recordingStatus.textContent = '녹음 시작하려면 클릭';
        }
        
        updateProgress();
        
        setTimeout(() => {
            loadQuestion(currentQuestionIndex + 1);
        }, 1500);
        
    } catch (error) {
        console.error('음성 답변 제출 실패:', error);
        alert('음성 답변 제출에 실패했습니다: ' + error.message);
        document.getElementById('ai-status').textContent = '실패';
        document.getElementById('ai-status').style.color = '#ef4444';
    }
};

function displayFeedback(feedback, question, questionNumber) {
    const feedbackList = document.getElementById('ai-feedback-list');
    
    if (feedbackList.querySelector('.empty-state')) {
        feedbackList.innerHTML = '';
    }

    const feedbackCard = document.createElement('div');
    feedbackCard.style.cssText = `
        background: white;
        border: 1px solid #e5e7eb;
        border-left: 4px solid #667eea;
        border-radius: 8px;
        padding: 1.25rem;
        margin-bottom: 1rem;
        animation: slideIn 0.3s ease-out;
    `;

    feedbackCard.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem;">
            <div style="font-weight: 600; color: #374151; font-size: 0.875rem;">
                Q${questionNumber}: ${question.questionText.substring(0, 40)}...
            </div>
            <div style="background: #667eea; color: white; padding: 0.25rem 0.75rem; border-radius: 6px; font-size: 0.875rem; font-weight: 600;">
                ${feedback.score}점
            </div>
        </div>
        <div style="background: #f9fafb; padding: 1rem; border-radius: 6px; margin-bottom: 0.75rem;">
            <div style="font-weight: 600; color: #10b981; font-size: 0.875rem; margin-bottom: 0.5rem;">✅ 강점</div>
            <div style="color: #374151; font-size: 0.875rem; line-height: 1.5;">
                ${feedback.strengths || '분석 중...'}
            </div>
        </div>
        <div style="background: #fef3c7; padding: 1rem; border-radius: 6px;">
            <div style="font-weight: 600; color: #f59e0b; font-size: 0.875rem; margin-bottom: 0.5rem;">💡 개선점</div>
            <div style="color: #374151; font-size: 0.875rem; line-height: 1.5;">
                ${feedback.improvementSuggestions || feedback.improvements || '분석 중...'}
            </div>
        </div>
    `;

    feedbackList.insertBefore(feedbackCard, feedbackList.firstChild);
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

    const avgScoreEl = document.getElementById('avg-score');
    const answeredCountEl = document.getElementById('answered-count');
    const totalTimeEl = document.getElementById('total-time');
    
    if (avgScoreEl) avgScoreEl.textContent = `${avgScore}점`;
    if (answeredCountEl) answeredCountEl.textContent = `${answers.length}개`;
    if (totalTimeEl) totalTimeEl.textContent = `${totalTime}분`;
}

function toggleResultView() {
    completeInterview();
}

function viewDetailedResults() {
    if (SESSION_DATA && SESSION_DATA.sessionId) {
        window.location.href = `/session/detail/${SESSION_DATA.sessionId}`;
    }
}
