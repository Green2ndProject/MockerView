class MockerViewWebSocket {
    constructor(sessionId, userId, userName) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.userName = userName;
        this.stompClient = null;
        this.connected = false;
    }

    connect() {
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);
        
        this.stompClient.connect({}, (frame) => {
            console.log('Connected: ' + frame);
            this.connected = true;
            
            this.subscribeToTopics();
            this.joinSession();
        }, (error) => {
            console.error('Connection error: ', error);
        });
    }

    subscribeToTopics() {
        this.stompClient.subscribe(`/topic/session/${this.sessionId}/status`, (message) => {
            this.handleStatusUpdate(JSON.parse(message.body));
        });

        this.stompClient.subscribe(`/topic/session/${this.sessionId}/question`, (message) => {
            this.handleNewQuestion(JSON.parse(message.body));
        });

        this.stompClient.subscribe(`/topic/session/${this.sessionId}/answer`, (message) => {
            this.handleNewAnswer(JSON.parse(message.body));
        });

        this.stompClient.subscribe(`/topic/session/${this.sessionId}/feedback`, (message) => {
            this.handleNewFeedback(JSON.parse(message.body));
        });

        this.stompClient.subscribe(`/topic/session/${this.sessionId}/interviewer-feedback`, (message) => {
            this.handleInterviewerFeedback(JSON.parse(message.body));
        });
    }

    joinSession() {
        if (this.connected) {
            this.stompClient.send(`/app/session/${this.sessionId}/join`, {}, JSON.stringify({
                sessionId: this.sessionId,
                userId: this.userId,
                userName: this.userName,
                action: 'JOIN'
            }));
        }
    }

    leaveSession() {
        if (this.connected) {
            this.stompClient.send(`/app/session/${this.sessionId}/leave`, {}, JSON.stringify({
                sessionId: this.sessionId,
                userId: this.userId,
                userName: this.userName,
                action: 'LEAVE'
            }));
        }
    }

    sendQuestion(questionId, questionText, orderNo) {
        if (this.connected) {
            this.stompClient.send(`/app/session/${this.sessionId}/question`, {}, JSON.stringify({
                sessionId: this.sessionId,
                questionId: questionId,
                questionText: questionText,
                orderNo: orderNo
            }));
        }
    }

    submitAnswer(questionId, answerText, score) {
        if (this.connected) {
            this.stompClient.send(`/app/session/${this.sessionId}/answer`, {}, JSON.stringify({
                sessionId: this.sessionId,
                questionId: questionId,
                userId: this.userId,
                userName: this.userName,
                answerText: answerText,
                score: score
            }));
        }
    }

    submitInterviewerFeedback(answerId, score, comment) {
        if (this.connected) {
            this.stompClient.send(`/app/session/${this.sessionId}/interviewer-feedback`, {}, JSON.stringify({
                answerId: answerId,
                reviewerId: this.userId,
                reviewerName: this.userName,
                score: score,
                comment: comment
            }));
        }
    }

    getSessionStatus() {
        if (this.connected) {
            this.stompClient.send(`/app/session/${this.sessionId}/status`, {}, JSON.stringify({
                sessionId: this.sessionId
            }));
        }
    }

    handleStatusUpdate(message) {
        console.log('Status update:', message);
        
        if (message.participants) {
            this.updateParticipantsList(message.participants);
        }
        
        if (message.questionCount !== undefined && message.answerCount !== undefined) {
            this.updateSessionStats(message.questionCount, message.answerCount);
        }
        
        if (message.action === 'JOIN') {
            this.showNotification(`${message.userName}님이 참가했습니다.`);
        } else if (message.action === 'LEAVE') {
            this.showNotification(`${message.userName}님이 퇴장했습니다.`);
        }
    }

    handleNewQuestion(message) {
        console.log('New question:', message);
        this.displayQuestion(message.questionText, message.orderNo);
        
        const questionIdInput = document.getElementById('questionId');
        if (questionIdInput) {
            questionIdInput.value = message.questionId || '';
        }
    }

    handleNewAnswer(message) {
        console.log('New answer:', message);
        this.displayAnswer(message);
        this.showNotification(`${message.userName}님이 답변을 제출했습니다.`);
    }

    handleNewFeedback(message) {
        console.log('New feedback:', message);
        this.displayAIFeedback(message);
        this.showNotification('AI 피드백이 생성되었습니다.');
    }

    handleInterviewerFeedback(message) {
        console.log('Interviewer feedback:', message);
        this.displayInterviewerFeedback(message);
        this.showNotification(`${message.reviewerName}님이 면접관 피드백을 주었습니다.`);
    }

    updateParticipantsList(participants) {
        const participantsList = document.getElementById('participants-list');
        if (participantsList && participants && Array.isArray(participants)) {
            participantsList.innerHTML = participants.map(name => 
                `<li class="list-group-item">${name}</li>`
            ).join('');
        }
    }

    updateSessionStats(questionCount, answerCount) {
        const statsDiv = document.getElementById('session-stats');
        if (statsDiv) {
            statsDiv.innerHTML = `질문: ${questionCount || 0}개 | 답변: ${answerCount || 0}개`;
        }
    }

    displayQuestion(questionText, orderNo) {
        const questionDiv = document.getElementById('current-question');
        if (questionDiv) {
            questionDiv.innerHTML = `
                <div class="card">
                    <div class="card-header bg-info text-white">
                        <h5>질문 ${orderNo}</h5>
                    </div>
                    <div class="card-body">
                        <p class="card-text">${questionText}</p>
                    </div>
                </div>
            `;
        }
    }

    displayAnswer(answer) {
        const answersDiv = document.getElementById('answers-list');
        if (answersDiv) {
            const answerElement = document.createElement('div');
            answerElement.className = 'answer-card mb-3';
            answerElement.id = `answer-${answer.answerId}`;
            answerElement.innerHTML = `
                <div class="card border-primary">
                    <div class="card-header">
                        <h6 class="mb-0">${answer.userName}</h6>
                        <small class="text-muted">${new Date().toLocaleTimeString()}</small>
                    </div>
                    <div class="card-body">
                        <p class="card-text">${answer.answerText}</p>
                        <div class="ai-feedback-placeholder">
                            <div class="spinner-border spinner-border-sm text-primary" role="status">
                                <span class="visually-hidden">AI 피드백 생성 중...</span>
                            </div>
                            <span class="ms-2 text-muted">AI 피드백 생성 중...</span>
                        </div>
                        ${this.getUserRole() === 'HOST' || this.getUserRole() === 'REVIEWER' ? this.getInterviewerFeedbackForm(answer.answerId) : ''}
                    </div>
                </div>
            `;
            answersDiv.appendChild(answerElement);
            
            if (this.getUserRole() === 'HOST' || this.getUserRole() === 'REVIEWER') {
                this.attachFeedbackFormHandlers(answer.answerId);
            }
        }
    }

    getInterviewerFeedbackForm(answerId) {
        return `
            <div class="interviewer-feedback-form mt-3 border-top pt-3">
                <h6 class="text-warning">면접관 피드백</h6>
                <div class="row">
                    <div class="col-md-6">
                        <label class="form-label">점수 (1-10)</label>
                        <select class="form-select form-select-sm score-input">
                            <option value="">선택</option>
                            ${Array.from({length: 10}, (_, i) => `<option value="${i+1}">${i+1}점</option>`).join('')}
                        </select>
                    </div>
                    <div class="col-md-6">
                        <button class="btn btn-warning btn-sm submit-feedback-btn" data-answer-id="${answerId}">
                            피드백 제출
                        </button>
                    </div>
                </div>
                <div class="mt-2">
                    <label class="form-label">코멘트</label>
                    <textarea class="form-control form-control-sm comment-input" rows="2" placeholder="피드백을 입력하세요..."></textarea>
                </div>
            </div>
        `;
    }

    attachFeedbackFormHandlers(answerId) {
        const answerElement = document.getElementById(`answer-${answerId}`);
        const submitBtn = answerElement.querySelector('.submit-feedback-btn');
        
        if (submitBtn) {
            submitBtn.addEventListener('click', () => {
                const scoreInput = answerElement.querySelector('.score-input');
                const commentInput = answerElement.querySelector('.comment-input');
                
                const score = parseInt(scoreInput.value);
                const comment = commentInput.value.trim();
                
                if (!score || score < 1 || score > 10) {
                    alert('점수를 1-10 사이로 선택해주세요.');
                    return;
                }
                
                if (!comment) {
                    alert('코멘트를 입력해주세요.');
                    return;
                }
                
                this.submitInterviewerFeedback(answerId, score, comment);
                
                scoreInput.value = '';
                commentInput.value = '';
                
                const feedbackForm = answerElement.querySelector('.interviewer-feedback-form');
                feedbackForm.innerHTML = '<div class="text-success">피드백이 제출되었습니다.</div>';
            });
        }
    }

    displayAIFeedback(feedback) {
        const answerElement = document.getElementById(`answer-${feedback.answerId}`);
        if (answerElement) {
            const placeholder = answerElement.querySelector('.ai-feedback-placeholder');
            if (placeholder) {
                placeholder.innerHTML = `
                    <div class="ai-feedback mt-2 p-2 bg-light rounded">
                        <h6 class="text-primary mb-2">🤖 AI 피드백</h6>
                        <div class="row">
                            <div class="col-md-6">
                                <strong>요약:</strong> ${feedback.summary}<br>
                                <strong class="text-success">강점:</strong> ${feedback.strengths}
                            </div>
                            <div class="col-md-6">
                                <strong class="text-warning">약점:</strong> ${feedback.weaknesses}<br>
                                <strong class="text-info">개선방안:</strong> ${feedback.improvement}
                            </div>
                        </div>
                        <small class="text-muted">${feedback.model} - ${new Date().toLocaleTimeString()}</small>
                    </div>
                `;
            }
        }
    }

    displayInterviewerFeedback(feedback) {
        const answerElement = document.getElementById(`answer-${feedback.answerId}`);
        if (answerElement) {
            const existingInterviewerFeedback = answerElement.querySelector('.interviewer-feedback-result');
            if (existingInterviewerFeedback) {
                existingInterviewerFeedback.remove();
            }
            
            const feedbackHtml = `
                <div class="interviewer-feedback-result mt-2 p-2 bg-warning bg-opacity-25 rounded">
                    <h6 class="text-warning mb-2">👨‍💼 면접관 피드백 - ${feedback.reviewerName}</h6>
                    <div>
                        <strong>점수:</strong> <span class="badge bg-warning text-dark">${feedback.score}/10</span>
                    </div>
                    <div class="mt-1">
                        <strong>코멘트:</strong> ${feedback.comment}
                    </div>
                    <small class="text-muted">${new Date().toLocaleTimeString()}</small>
                </div>
            `;
            
            answerElement.querySelector('.card-body').insertAdjacentHTML('beforeend', feedbackHtml);
        }
    }

    getUserRole() {
        const userRoleInput = document.getElementById('userRole');
        return userRoleInput ? userRoleInput.value : 'STUDENT';
    }

    showNotification(message) {
        const toast = document.createElement('div');
        toast.className = 'toast position-fixed top-0 end-0 m-3';
        toast.innerHTML = `
            <div class="toast-body">
                ${message}
            </div>
        `;
        document.body.appendChild(toast);
        
        const bsToast = new bootstrap.Toast(toast);
        bsToast.show();
        
        setTimeout(() => {
            document.body.removeChild(toast);
        }, 5000);
    }

    disconnect() {
        if (this.connected) {
            this.leaveSession();
            this.stompClient.disconnect();
            this.connected = false;
        }
    }
}

let mockerViewWS = null;

function initializeWebSocket(sessionId, userId, userName) {
    mockerViewWS = new MockerViewWebSocket(sessionId, userId, userName);
    mockerViewWS.connect();
    
    window.addEventListener('beforeunload', () => {
        if (mockerViewWS) {
            mockerViewWS.disconnect();
        }
    });
}

function submitAnswer() {
    const questionId = document.getElementById('questionId').value;
    const answerText = document.getElementById('answerText').value;
    
    if (answerText.trim() && mockerViewWS) {
        mockerViewWS.submitAnswer(questionId, answerText, null);
        document.getElementById('answerText').value = '';
    }
}

function sendQuestion() {
    const questionId = document.getElementById('newQuestionId').value;
    const questionText = document.getElementById('newQuestionText').value;
    const orderNo = document.getElementById('newQuestionOrder').value;
    
    if (questionText.trim() && mockerViewWS) {
        mockerViewWS.sendQuestion(questionId, questionText, parseInt(orderNo));
        document.getElementById('newQuestionText').value = '';
        document.getElementById('newQuestionOrder').value = '';
    }
}