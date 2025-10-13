class MockerViewWebSocket {
  constructor(sessionId, userId, userName) {
    this.sessionId = sessionId;
    this.userId = userId;
    this.userName = userName;
    this.stompClient = null;
    this.connected = false;
    this.timerInterval = null;
    this.currentSeconds = 0;
  }

  connect() {
    const token = this.getTokenFromCookie();
    if (!token) {
      console.error('❌ 토큰 없음');
      alert('인증 토큰이 없습니다. 다시 로그인해주세요.');
      return;
    }
    
    console.log('🔌 WebSocket 연결 시도...', {
      sessionId: this.sessionId,
      userId: this.userId,
      userName: this.userName
    });
    
    const socket = new SockJS("/ws?token=" + encodeURIComponent(token));
    this.stompClient = Stomp.over(socket);
    
    this.stompClient.debug = null;
    
    this.stompClient.connect({}, (frame) => {
      console.log("✅ WebSocket STOMP 연결 성공");
      this.connected = true;
      
      this.subscribeToTopics();
      
      setTimeout(() => {
        this.joinSession();
      }, 100);
      
    }, (error) => {
      console.error("❌ WebSocket 연결 실패:", error);
      alert('WebSocket 연결에 실패했습니다.');
      this.connected = false;
    });
  }

  getTokenFromCookie() {
    const cookies = document.cookie.split(';');
    for (let cookie of cookies) {
      const [name, value] = cookie.trim().split('=');
      if (name === 'Authorization') return value;
    }
    return null;
  }

  subscribeToTopics() {
    console.log('📡 토픽 구독 시작...');
    
    this.stompClient.subscribe(`/topic/session/${this.sessionId}/status`, (message) => {
      console.log('📊 Status 메시지 수신');
      this.handleStatusUpdate(JSON.parse(message.body));
    });
    
    this.stompClient.subscribe(`/topic/session/${this.sessionId}/question`, (message) => {
      console.log('❓ Question 메시지 수신');
      this.handleNewQuestion(JSON.parse(message.body));
    });
    
    this.stompClient.subscribe(`/topic/session/${this.sessionId}/answer`, (message) => {
      console.log('💬 Answer 메시지 수신');
      this.handleNewAnswer(JSON.parse(message.body));
    });
    
    this.stompClient.subscribe(`/topic/session/${this.sessionId}/feedback`, (message) => {
      console.log('🤖 Feedback 메시지 수신');
      this.handleNewFeedback(JSON.parse(message.body));
    });
    
    this.stompClient.subscribe(`/topic/session/${this.sessionId}/interviewer-feedback`, (message) => {
      console.log('👔 Interviewer Feedback 메시지 수신');
      this.handleInterviewerFeedback(JSON.parse(message.body));
    });
    
    this.stompClient.subscribe(`/topic/session/${this.sessionId}/control`, (message) => {
      console.log('🎮 Control 메시지 수신');
      this.handleControlMessage(JSON.parse(message.body));
    });
    
    console.log('✅ 모든 토픽 구독 완료');
  }

  joinSession() {
    if (!this.connected) {
      console.warn('⚠️ WebSocket이 아직 연결되지 않았습니다');
      return;
    }
    
    if (!this.stompClient) {
      console.error('❌ stompClient가 null입니다');
      return;
    }
    
    const joinMessage = {
      sessionId: this.sessionId,
      userId: this.userId,
      userName: this.userName,
      action: "JOIN"
    };
    
    console.log('📨 세션 참가 메시지 전송:', joinMessage);
    console.log('📍 전송 경로:', `/app/session/${this.sessionId}/join`);
    
    try {
      this.stompClient.send(
        `/app/session/${this.sessionId}/join`, 
        {}, 
        JSON.stringify(joinMessage)
      );
      console.log('✅ 세션 참가 메시지 전송 완료');
    } catch (error) {
      console.error('❌ 세션 참가 메시지 전송 실패:', error);
    }
  }

  handleControlMessage(data) {
    console.log('🎮 제어 메시지 처리:', data);
    const badge = document.getElementById('sessionStatusBadge');
    
    if (data.action === 'START') {
      alert('면접이 시작되었습니다!');
      if (badge) {
        badge.textContent = '진행중';
        badge.className = 'status-badge ongoing';
      }
    } else if (data.action === 'PAUSE') {
      alert('면접이 일시정지되었습니다.');
      if (badge) {
        badge.textContent = '일시정지';
        badge.className = 'status-badge paused';
      }
    } else if (data.action === 'RESUME') {
      alert('면접이 재개되었습니다.');
      if (badge) {
        badge.textContent = '진행중';
        badge.className = 'status-badge ongoing';
      }
    } else if (data.action === 'END') {
      alert('면접이 종료되었습니다.');
      if (badge) {
        badge.textContent = '종료됨';
        badge.className = 'status-badge ended';
      }
    }
  }

  leaveSession() {
    if (this.connected && this.stompClient) {
      console.log('👋 세션 퇴장 메시지 전송');
      this.stompClient.send(`/app/session/${this.sessionId}/leave`, {}, JSON.stringify({
        sessionId: this.sessionId,
        userId: this.userId,
        userName: this.userName,
        action: "LEAVE"
      }));
    }
  }

  sendQuestion(questionText, orderNo, timer) {
    if (!this.connected) {
      alert('WebSocket이 연결되지 않았습니다.');
      return;
    }
    
    const payload = {
      text: questionText,
      orderNo: parseInt(orderNo) || 1,
      timerSeconds: parseInt(timer) || 60,
      sessionId: this.sessionId
    };
    
    console.log('📤 질문 전송:', payload);
    this.stompClient.send(`/app/session/${this.sessionId}/question`, {}, JSON.stringify(payload));
  }

  sendControlMessage(action) {
    if (!this.connected) {
      console.warn('⚠️ WebSocket이 연결되지 않았습니다');
      return;
    }
    
    console.log('🎮 제어 메시지 전송:', action);
    this.stompClient.send(`/app/session/${this.sessionId}/control`, {}, JSON.stringify({
      action: action,
      timestamp: new Date().toISOString()
    }));
  }

  stopTimer() {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  submitAnswer(questionId, answerText) {
    if (!this.connected) {
      alert('WebSocket이 연결되지 않았습니다.');
      return;
    }
    
    const payload = {
      sessionId: this.sessionId,
      questionId: parseInt(questionId),
      userId: this.userId,
      userName: this.userName,
      answerText: answerText
    };
    
    console.log('📤 답변 제출:', payload);
    this.stompClient.send(`/app/session/${this.sessionId}/answer`, {}, JSON.stringify(payload));
  }

  submitInterviewerFeedback(answerId, score, comment) {
    if (!this.connected) {
      alert('WebSocket이 연결되지 않았습니다.');
      return;
    }
    
    this.stompClient.send(`/app/session/${this.sessionId}/interviewer-feedback`, {}, JSON.stringify({
      sessionId: this.sessionId,
      answerId: answerId,
      reviewerId: this.userId,
      reviewerName: this.userName,
      score: score,
      comment: comment
    }));
  }

  handleStatusUpdate(message) {
    console.log('📊 상태 업데이트:', message);
    
    if (message.participants && Array.isArray(message.participants)) {
      this.updateParticipantsList(message.participants);
    }
    
    if (message.questionCount !== undefined && message.answerCount !== undefined) {
      this.updateSessionStats(message.questionCount, message.answerCount);
    }
    
    if (message.action === "JOIN" && message.userName) {
      this.showNotification(message.userName + "님이 입장했습니다. 👋");
    } else if (message.action === "LEAVE" && message.userName) {
      this.showNotification(message.userName + "님이 퇴장했습니다. 👋");
    }
    
    if (message.status) {
      const badge = document.getElementById('sessionStatusBadge');
      if (badge) {
        const statusMap = {
          'RUNNING': { text: '진행중', className: 'status-badge ongoing' },
          'PAUSED': { text: '일시정지', className: 'status-badge paused' },
          'ENDED': { text: '종료됨', className: 'status-badge ended' },
          'DEFAULT': { text: '대기중', className: 'status-badge waiting' }
        };
        const status = statusMap[message.status] || statusMap['DEFAULT'];
        badge.textContent = status.text;
        badge.className = status.className;
      }
    }
  }

  handleNewQuestion(message) {
    console.log("❓ 새 질문 처리:", message);
    
    const questionTextElement = document.getElementById('current-question-text');
    if (questionTextElement) {
      questionTextElement.textContent = message.questionText || message.text || '질문을 불러오는 중...';
    }
    
    const questionIdInput = document.getElementById("currentQuestionId");
    if (questionIdInput) {
      questionIdInput.value = message.questionId || message.id || "";
    }
    
    const questionNumber = document.getElementById("question-number");
    if (questionNumber) {
      questionNumber.textContent = "Q" + (message.orderNo || message.order || 1);
    }
    
    const timerSeconds = message.timer || message.timerSeconds;
    if (timerSeconds && timerSeconds > 0) {
      this.currentSeconds = timerSeconds;
      this.stopTimer();
      
      const timerElement = document.getElementById('question-timer');
      this.timerInterval = setInterval(() => {
        if (this.currentSeconds > 0) {
          this.currentSeconds--;
          const minutes = Math.floor(this.currentSeconds / 60);
          const seconds = this.currentSeconds % 60;
          
          if (timerElement) {
            timerElement.textContent = "⏱️ " + minutes + ":" + seconds.toString().padStart(2, '0');
            
            if (this.currentSeconds <= 10) {
              timerElement.style.color = '#ef4444';
            } else if (this.currentSeconds <= 30) {
              timerElement.style.color = '#ff9800';
            }
          }
        } else {
          this.stopTimer();
          alert('⏰ 답변 시간이 종료되었습니다!');
        }
      }, 1000);
    }
  }

  handleNewAnswer(message) {
    console.log("💬 새 답변 처리:", message);
    this.displayAnswer(message);
    this.showNotification(message.userName + "님이 답변을 제출했습니다.");
    
    if (message.answerId || message.id) {
      this.requestAIFeedback(message.answerId || message.id);
    }
  }

  requestAIFeedback(answerId) {
    const token = this.getTokenFromCookie();
    fetch("/api/feedback/ai/" + answerId, {
      method: 'POST',
      headers: { 
        'Authorization': token,
        'Content-Type': 'application/json'
      }
    }).then(response => {
      if (response.ok) return response.json();
    }).then(feedback => {
      console.log('✅ AI 피드백 요청 완료:', feedback);
    }).catch(error => {
      console.error('❌ AI 피드백 실패:', error);
    });
  }

  handleNewFeedback(message) {
    console.log("🤖 AI 피드백 처리:", message);
    this.displayAIFeedback(message);
    this.showNotification("AI 피드백이 생성되었습니다.");
  }

  handleInterviewerFeedback(message) {
    console.log("👔 면접관 피드백 처리:", message);
    this.displayInterviewerFeedback(message);
    this.showNotification(message.reviewerName + "님이 평가했습니다.");
  }

  updateParticipantsList(participants) {
    console.log("👥 참가자 목록 업데이트:", participants);
    
    const participantsListDiv = document.getElementById("participants-list");
    if (!participantsListDiv) return;
    
    if (!participants || participants.length === 0) {
        participantsListDiv.innerHTML = '<div class="empty-state">대기 중...</div>';
        return;
    }
    
    participantsListDiv.innerHTML = participants.map(participant => 
        `<div class="participant-item">
            <div class="participant-avatar">${participant.charAt(0).toUpperCase()}</div>
            <span>${participant}</span>
        </div>`
    ).join("");
    
    const participantCount = document.getElementById("participant-count");
    if (participantCount) {
        participantCount.textContent = (participants.length + 1) + "명";
    }
}

  updateSessionStats(questionCount, answerCount) {
    const statsDiv = document.getElementById("session-stats");
    if (statsDiv) {
      statsDiv.innerHTML = `질문 ${questionCount || 0}개 • 답변 ${answerCount || 0}개`;
    }
    
    const answerCountSpan = document.getElementById("answer-count");
    if (answerCountSpan) {
      answerCountSpan.textContent = `${answerCount || 0}개`;
    }
  }

  displayAnswer(answer) {
    const answersDiv = document.getElementById("answers-list");
    if (answersDiv) {
      const answerId = answer.answerId || answer.id;
      const existingAnswer = document.getElementById("answer-" + answerId);
      if (existingAnswer) return;
      
      const emptyState = answersDiv.querySelector(".empty-state");
      if (emptyState) emptyState.remove();
      
      const answerElement = document.createElement("div");
      answerElement.className = "answer-review-card";
      answerElement.id = "answer-" + answerId;
      answerElement.innerHTML = `
        <div class="answer-header">
          <div class="user-name-badge">${answer.userName}</div>
          <div class="answer-time">${new Date().toLocaleTimeString()}</div>
        </div>
        <div class="answer-text">${answer.answerText}</div>
        <div class="ai-feedback-section" id="ai-feedback-${answerId}">
          <div class="ai-feedback-loading">🤖 AI 분석 중...</div>
        </div>
        <div class="interviewer-evaluation" id="evaluation-${answerId}">
          <div class="evaluation-header"><h5>면접관 평가</h5></div>
          <div class="score-input-group">
            <label>점수 (1-100):</label>
            <input type="number" min="1" max="100" value="70" id="score-${answerId}" class="score-input">
          </div>
          <div class="feedback-input-group">
            <label>피드백:</label>
            <textarea placeholder="이 답변에 대한 피드백을 작성하세요..." id="feedback-${answerId}" class="feedback-textarea"></textarea>
          </div>
          <button onclick="submitInterviewerFeedback(${answerId})" class="evaluation-submit-btn">평가 제출</button>
        </div>
      `;
      answersDiv.appendChild(answerElement);
    }
    
    const aiFeedbackList = document.getElementById("ai-feedback-list");
    if (aiFeedbackList) {
      const answerId = answer.answerId || answer.id;
      const existingFeedback = document.getElementById("student-ai-feedback-" + answerId);
      if (existingFeedback) return;
      
      const emptyState = aiFeedbackList.querySelector(".empty-state");
      if (emptyState) emptyState.remove();
      
      const feedbackCard = document.createElement("div");
      feedbackCard.className = "ai-feedback-section";
      feedbackCard.id = "student-ai-feedback-" + answerId;
      feedbackCard.innerHTML = '<div class="ai-feedback-loading">🤖 AI가 답변을 분석하는 중...</div>';
      aiFeedbackList.appendChild(feedbackCard);
    }
  }

  displayAIFeedback(feedback) {
    const answerId = feedback.answerId || feedback.id;
    
    const aiSection = document.getElementById("ai-feedback-" + answerId);
    if (aiSection) {
        aiSection.innerHTML = `
            <div class="ai-feedback-content">
                <div class="ai-feedback-header">🤖 AI 분석 결과</div>
                <div class="ai-score">점수: ${feedback.score || 75}/100</div>
                <div class="ai-strengths"><strong>강점:</strong> ${feedback.strengths || '분석 중...'}</div>
                <div class="ai-improvements"><strong>개선점:</strong> ${feedback.weaknesses || feedback.improvements || '분석 중...'}</div>
            </div>
        `;
    }
    
    const studentAiSection = document.getElementById("student-ai-feedback-" + answerId);
    if (studentAiSection) {
        studentAiSection.innerHTML = `
            <div class="ai-feedback-content">
                <div class="ai-feedback-header">🤖 AI 분석 결과</div>
                <div class="ai-score">점수: ${feedback.score || 75}/100</div>
                <div class="ai-strengths"><strong>강점:</strong> ${feedback.strengths || '분석 중...'}</div>
                <div class="ai-improvements"><strong>개선점:</strong> ${feedback.weaknesses || feedback.improvements || '분석 중...'}</div>
            </div>
        `;
    }
}

  displayInterviewerFeedback(feedback) {
    const answerId = feedback.answerId || feedback.id;
    const answerElement = document.getElementById("answer-" + answerId);
    if (!answerElement) return;
    
    const evaluationDiv = document.getElementById("evaluation-" + answerId);
    if (evaluationDiv) evaluationDiv.remove();
    
    const existingFeedback = answerElement.querySelectorAll(".interviewer-feedback");
    existingFeedback.forEach(el => el.remove());
    
    const feedbackHtml = `
      <div class="interviewer-feedback">
        <div class="interviewer-feedback-title">👔 면접관 피드백</div>
        <div class="interviewer-name">면접관: ${feedback.reviewerName}</div>
        <div class="feedback-score">
          <strong>점수:</strong> <span class="score-badge">${feedback.score}/100</span>
        </div>
        <div class="feedback-comment"><strong>코멘트:</strong> ${feedback.comment}</div>
        <div class="text-muted">${new Date().toLocaleTimeString()}</div>
      </div>
    `;
    answerElement.insertAdjacentHTML("beforeend", feedbackHtml);
  }

  showNotification(message) {
    console.log('📢 알림:', message);
    const notificationDiv = document.createElement('div');
    notificationDiv.textContent = message;
    notificationDiv.style.cssText = 'position: fixed; top: 80px; right: 20px; background: #667eea; color: white; padding: 12px 20px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.15); z-index: 10000; animation: slideIn 0.3s ease-out; font-weight: 500;';
    document.body.appendChild(notificationDiv);
    setTimeout(() => {
      notificationDiv.style.animation = 'slideOut 0.3s ease-in';
      setTimeout(() => document.body.removeChild(notificationDiv), 300);
    }, 3000);
  }

  disconnect() {
    if (this.connected) {
      this.leaveSession();
      this.stompClient.disconnect();
      this.connected = false;
    }
  }
}

window.sendQuestion = function() {
  console.log('📝 sendQuestion 호출됨');
  const questionText = document.getElementById("newQuestionText").value;
  const orderNo = document.getElementById("newQuestionOrder").value;
  const timerOrder = document.getElementById("newTimerOrder").value;
  
  if (!questionText.trim()) {
    alert("질문을 입력해주세요.");
    return;
  }
  
  if (window.mockerViewWS && window.mockerViewWS.connected) {
    window.mockerViewWS.sendQuestion(questionText, parseInt(orderNo) || 1, parseInt(timerOrder) || 60);
    document.getElementById("newQuestionText").value = "";
    document.getElementById("newQuestionOrder").value = parseInt(orderNo) + 1;
  } else {
    alert("WebSocket이 연결되지 않았습니다.");
  }
};

window.submitAnswer = function() {
  console.log('💬 submitAnswer 호출됨');
  const questionId = document.getElementById("currentQuestionId").value;
  const answerText = document.getElementById("answerText").value;
  
  if (!answerText.trim()) {
    alert("답변을 입력해주세요.");
    return;
  }
  
  if (!questionId) {
    alert("현재 답변할 질문이 없습니다.");
    return;
  }
  
  if (window.mockerViewWS && window.mockerViewWS.connected) {
    window.mockerViewWS.submitAnswer(questionId, answerText);
    document.getElementById("answerText").value = "";
    alert('✅ 답변이 제출되었습니다!');
  } else {
    alert("WebSocket이 연결되지 않았습니다.");
  }
};

window.submitInterviewerFeedback = function(answerId) {
  console.log('👔 submitInterviewerFeedback 호출됨:', answerId);
  const scoreInput = document.getElementById("score-" + answerId);
  const feedbackTextarea = document.getElementById("feedback-" + answerId);
  const evaluationDiv = document.getElementById("evaluation-" + answerId);
  
  if (!scoreInput || !feedbackTextarea) return;
  
  const score = scoreInput.value;
  const feedbackText = feedbackTextarea.value;
  
  if (!feedbackText.trim()) {
    alert('피드백을 입력해주세요.');
    return;
  }
  
  const submitBtn = evaluationDiv ? evaluationDiv.querySelector(".evaluation-submit-btn") : null;
  if (submitBtn && submitBtn.disabled) return;
  
  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.textContent = '제출 중...';
  }
  
  if (window.mockerViewWS && window.mockerViewWS.connected) {
    window.mockerViewWS.submitInterviewerFeedback(answerId, parseInt(score), feedbackText);
    setTimeout(() => {
      if (evaluationDiv) {
        evaluationDiv.innerHTML = `<div class="submitted-evaluation">✅ 평가 완료 (점수: ${score}/100)</div>`;
      }
    }, 300);
  }
};