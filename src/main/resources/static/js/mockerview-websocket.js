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
      console.error('토큰을 찾을 수 없습니다!');
      alert('인증 토큰이 없습니다. 다시 로그인해주세요.');
      return;
    }
    console.log('토큰 전송:', token.substring(0, 20) + '...');
    const socket = new SockJS("/ws?token=" + encodeURIComponent(token));
    this.stompClient = Stomp.over(socket);
    this.stompClient.connect({}, (frame) => {
      console.log("WebSocket 연결 성공");
      this.connected = true;
      window.stompClient = this.stompClient;
      window.mockerViewWS = this;
      this.subscribeToTopics();
      this.joinSession();
    }, (error) => {
      console.error("WebSocket 연결 실패:", error);
      alert('WebSocket 연결에 실패했습니다. 페이지를 새로고침 해주세요.');
      this.connected = false;
    });
  }

  getTokenFromCookie() {
    const cookies = document.cookie.split(';');
    for (let cookie of cookies) {
      const [name, value] = cookie.trim().split('=');
      if (name === 'Authorization') {
        return value;
      }
    }
    return null;
  }

  subscribeToTopics() {
    this.stompClient.subscribe(`/topic/session/${this.sessionId}/status`, (message) => {
      console.log('📊 Status 수신:', message.body);
      this.handleStatusUpdate(JSON.parse(message.body));
    });
    this.stompClient.subscribe(`/topic/session/${this.sessionId}/question`, (message) => {
      console.log('❓ 질문 수신:', message.body);
      this.handleNewQuestion(JSON.parse(message.body));
    });
    this.stompClient.subscribe(`/topic/session/${this.sessionId}/answer`, (message) => {
      console.log('💬 답변 수신:', message.body);
      this.handleNewAnswer(JSON.parse(message.body));
    });
    this.stompClient.subscribe(`/topic/session/${this.sessionId}/feedback`, (message) => {
      console.log('🤖 AI 피드백 수신:', message.body);
      this.handleNewFeedback(JSON.parse(message.body));
    });
    this.stompClient.subscribe(`/topic/session/${this.sessionId}/interviewer-feedback`, (message) => {
      console.log('👔 면접관 피드백 수신:', message.body);
      this.handleInterviewerFeedback(JSON.parse(message.body));
    });
    this.stompClient.subscribe(`/topic/session/${this.sessionId}/control`, (message) => {
      console.log('🎮 제어 메시지 수신:', message.body);
      this.handleControlMessage(JSON.parse(message.body));
    });
  }

  handleControlMessage(data) {
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

  joinSession() {
    if (this.connected) {
      this.stompClient.send(`/app/session/${this.sessionId}/join`, {}, JSON.stringify({
        sessionId: this.sessionId,
        userId: this.userId,
        userName: this.userName,
        action: "JOIN"
      }));
    }
  }

  leaveSession() {
    if (this.connected) {
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
    console.log('📤 질문 전송 시작:', { questionText, orderNo, timer });
    const payload = {
      text: questionText,
      orderNo: parseInt(orderNo) || 1,
      timerSeconds: parseInt(timer) || 60,
      sessionId: this.sessionId
    };
    console.log('📤 전송 데이터:', payload);
    this.stompClient.send(`/app/session/${this.sessionId}/question`, {}, JSON.stringify(payload));
  }

  sendControlMessage(action) {
    if (this.connected) {
      console.log('🎮 제어 메시지 전송:', action);
      this.stompClient.send(`/app/session/${this.sessionId}/control`, {}, JSON.stringify({
        action: action,
        timestamp: new Date().toISOString()
      }));
    }
  }

  stopTimer() {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  submitAnswer(questionId, answerText) {
    if (this.connected) {
      this.stompClient.send(`/app/session/${this.sessionId}/answer`, {}, JSON.stringify({
        sessionId: this.sessionId,
        questionId: parseInt(questionId),
        userId: this.userId,
        userName: this.userName,
        answerText: answerText
      }));
    }
  }

  submitInterviewerFeedback(answerId, score, comment) {
    if (this.connected) {
      this.stompClient.send(`/app/session/${this.sessionId}/interviewer-feedback`, {}, JSON.stringify({
        sessionId: this.sessionId,
        answerId: answerId,
        reviewerId: this.userId,
        reviewerName: this.userName,
        score: score,
        comment: comment
      }));
    }
  }

  handleStatusUpdate(message) {
    console.log("📊 Status update:", message);
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
        if (message.status === 'RUNNING') {
          badge.textContent = '진행중';
          badge.className = 'status-badge ongoing';
        } else if (message.status === 'PAUSED') {
          badge.textContent = '일시정지';
          badge.className = 'status-badge paused';
        } else if (message.status === 'ENDED') {
          badge.textContent = '종료됨';
          badge.className = 'status-badge ended';
        } else {
          badge.textContent = '대기중';
          badge.className = 'status-badge waiting';
        }
      }
    }
  }

  handleNewQuestion(message) {
    console.log("❓ 새 질문 처리:", message);
    const questionTextElement = document.getElementById('current-question-text');
    if (questionTextElement) {
      questionTextElement.textContent = message.questionText || '질문을 불러오는 중...';
    }
    const questionIdInput = document.getElementById("currentQuestionId");
    if (questionIdInput) {
      questionIdInput.value = message.questionId || "";
    }
    const questionNumber = document.getElementById("question-number");
    if (questionNumber) {
      questionNumber.textContent = "Q" + (message.orderNo || 1);
    }
    if (message.timer && message.timer > 0) {
      this.currentSeconds = message.timer;
      this.stopTimer();
      const timerElement = document.getElementById('question-timer');
      const self = this;
      this.timerInterval = setInterval(function() {
        if (self.currentSeconds > 0) {
          self.currentSeconds--;
          const minutes = Math.floor(self.currentSeconds / 60);
          const seconds = self.currentSeconds % 60;
          if (timerElement) {
            timerElement.textContent = "⏱️ " + minutes + ":" + seconds.toString().padStart(2, '0');
            if (self.currentSeconds <= 10) {
              timerElement.style.color = '#ef4444';
            } else if (self.currentSeconds <= 30) {
              timerElement.style.color = '#ff9800';
            }
          }
        } else {
          self.stopTimer();
          alert('⏰ 답변 시간이 종료되었습니다!');
        }
      }, 1000);
    }
  }

  handleNewAnswer(message) {
    console.log("💬 새 답변 처리:", message);
    this.displayAnswer(message);
    this.showNotification(message.userName + "님이 답변을 제출했습니다.");
    if (message.answerId) {
      this.requestAIFeedback(message.answerId);
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
    }).then(function(response) {
      if (response.ok) {
        return response.json();
      }
    }).then(function(feedback) {
      console.log('✅ AI 피드백 생성됨:', feedback);
    }).catch(function(error) {
      console.error('❌ AI 피드백 요청 실패:', error);
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
    this.showNotification(message.reviewerName + "님이 면접관 피드백을 주었습니다.");
  }

  updateParticipantsList(participants) {
    console.log("👥 참가자 목록 업데이트:", participants);
    const participantsListDiv = document.getElementById("participants-list");
    if (!participantsListDiv) {
      return;
    }
    if (!participants || participants.length === 0) {
      participantsListDiv.innerHTML = '<div class="empty-state">참가자 없음</div>';
      return;
    }
    participantsListDiv.innerHTML = participants.map(function(participant) {
      return '<div class="participant-item"><div class="participant-avatar">' + 
        participant.charAt(0).toUpperCase() + 
        '</div><div class="participant-info"><div class="participant-name">' + 
        participant + 
        '</div><div class="participant-status">🟢 온라인</div></div></div>';
    }).join("");
    const participantCount = document.getElementById("participant-count");
    if (participantCount) {
      participantCount.textContent = "참가자 " + participants.length + "명";
    }
  }

  updateSessionStats(questionCount, answerCount) {
    const statsDiv = document.getElementById("session-stats");
    if (statsDiv) {
      statsDiv.innerHTML = "질문 " + (questionCount || 0) + "개 • 답변 " + (answerCount || 0) + "개";
    }
    const questionCountSpan = document.getElementById("question-count");
    if (questionCountSpan) {
      questionCountSpan.textContent = "질문 " + (questionCount || 0) + "개";
    }
    const answerCountSpan = document.getElementById("answer-count");
    if (answerCountSpan) {
      answerCountSpan.textContent = (answerCount || 0) + "개";
    }
  }

  displayAnswer(answer) {
    const answersDiv = document.getElementById("answers-list");
    if (answersDiv) {
      const existingAnswer = document.getElementById("answer-" + answer.answerId);
      if (existingAnswer) {
        console.log("이미 존재하는 답변 카드:", answer.answerId);
        return;
      }
      
      const emptyState = answersDiv.querySelector(".empty-state");
      if (emptyState) {
        emptyState.remove();
      }
      const answerElement = document.createElement("div");
      answerElement.className = "answer-review-card";
      answerElement.id = "answer-" + answer.answerId;
      answerElement.innerHTML = '<div class="answer-header"><div class="user-name-badge">' + answer.userName + '</div><div class="answer-time">' + new Date().toLocaleTimeString() + '</div></div><div class="answer-text">' + answer.answerText + '</div><div class="ai-feedback-section" id="ai-feedback-' + answer.answerId + '"><div class="ai-feedback-loading">🤖 AI 분석 중...</div></div><div class="interviewer-evaluation" id="evaluation-' + answer.answerId + '"><div class="evaluation-header"><h5>면접관 평가</h5></div><div class="score-input-group"><label>점수 (1-100):</label><input type="number" min="1" max="100" value="70" id="score-' + answer.answerId + '" class="score-input"></div><div class="feedback-input-group"><label>피드백:</label><textarea placeholder="이 답변에 대한 피드백을 작성하세요..." id="feedback-' + answer.answerId + '" class="feedback-textarea"></textarea></div><button onclick="window.submitInterviewerFeedback(' + answer.answerId + ')" class="evaluation-submit-btn">평가 제출</button></div>';
      answersDiv.appendChild(answerElement);
    }
    
    const aiFeedbackList = document.getElementById("ai-feedback-list");
    if (aiFeedbackList) {
      const existingFeedback = document.getElementById("student-ai-feedback-" + answer.answerId);
      if (existingFeedback) {
        console.log("이미 존재하는 AI 피드백 카드:", answer.answerId);
        return;
      }
      
      const emptyState = aiFeedbackList.querySelector(".empty-state");
      if (emptyState) {
        emptyState.remove();
      }
      const feedbackCard = document.createElement("div");
      feedbackCard.className = "ai-feedback-section";
      feedbackCard.id = "student-ai-feedback-" + answer.answerId;
      feedbackCard.innerHTML = '<div class="ai-feedback-loading">🤖 AI가 답변을 분석하는 중...</div>';
      aiFeedbackList.appendChild(feedbackCard);
    }
  }

  displayAIFeedback(feedback) {
    const aiSection = document.getElementById("ai-feedback-" + feedback.answerId);
    if (aiSection) {
      aiSection.innerHTML = '<div class="ai-feedback-content"><div class="ai-feedback-header">🤖 AI 분석 결과</div><div class="ai-score">점수: ' + (feedback.score || 75) + '/100</div><div class="ai-strengths"><strong>강점:</strong> ' + (feedback.strengths || '분석 중...') + '</div><div class="ai-improvements"><strong>개선점:</strong> ' + (feedback.weaknesses || feedback.improvements || '분석 중...') + '</div></div>';
    }
    const studentAiSection = document.getElementById("student-ai-feedback-" + feedback.answerId);
    if (studentAiSection) {
      studentAiSection.innerHTML = '<div class="ai-feedback-content"><div class="ai-feedback-header">🤖 AI 분석 결과</div><div class="ai-score">점수: ' + (feedback.score || 75) + '/100</div><div class="ai-strengths"><strong>강점:</strong> ' + (feedback.strengths || '분석 중...') + '</div><div class="ai-improvements"><strong>개선점:</strong> ' + (feedback.weaknesses || feedback.improvements || '분석 중...') + '</div></div>';
    }
  }

  displayInterviewerFeedback(feedback) {
    console.log("👔 면접관 피드백 표시:", feedback);
    const answerElement = document.getElementById("answer-" + feedback.answerId);
    if (!answerElement) {
      console.warn("답변 카드를 찾을 수 없습니다:", feedback.answerId);
      return;
    }
    
    const evaluationDiv = document.getElementById("evaluation-" + feedback.answerId);
    if (evaluationDiv) {
      evaluationDiv.remove();
    }
    
    const existingFeedback = answerElement.querySelectorAll(".interviewer-feedback");
    existingFeedback.forEach(function(el) {
      el.remove();
    });
    
    const feedbackHtml = '<div class="interviewer-feedback"><div class="interviewer-feedback-title">👔 면접관 피드백</div><div class="interviewer-name">면접관: ' + feedback.reviewerName + '</div><div class="feedback-score"><strong>점수:</strong> <span class="score-badge">' + feedback.score + '/100</span></div><div class="feedback-comment"><strong>코멘트:</strong> ' + feedback.comment + '</div><div class="text-muted">' + new Date().toLocaleTimeString() + '</div></div>';
    
    answerElement.insertAdjacentHTML("beforeend", feedbackHtml);
  }

  showNotification(message) {
    console.log('📢 알림:', message);
    const notificationDiv = document.createElement('div');
    notificationDiv.className = 'toast-notification';
    notificationDiv.textContent = message;
    notificationDiv.style.cssText = 'position: fixed; top: 80px; right: 20px; background: #667eea; color: white; padding: 12px 20px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.15); z-index: 10000; animation: slideIn 0.3s ease-out; font-weight: 500;';
    document.body.appendChild(notificationDiv);
    setTimeout(function() {
      notificationDiv.style.animation = 'slideOut 0.3s ease-in';
      setTimeout(function() {
        document.body.removeChild(notificationDiv);
      }, 300);
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

var mockerViewWS = null;

function initializeWebSocket(sessionId, userId, userName) {
  mockerViewWS = new MockerViewWebSocket(sessionId, userId, userName);
  window.mockerViewWS = mockerViewWS;
  mockerViewWS.connect();
  window.addEventListener("beforeunload", function() {
    if (mockerViewWS) {
      mockerViewWS.disconnect();
    }
  });
}

window.sendQuestion = function() {
  var questionText = document.getElementById("newQuestionText").value;
  var orderNo = document.getElementById("newQuestionOrder").value;
  var timerOrder = document.getElementById("newTimerOrder").value;
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
  var questionId = document.getElementById("currentQuestionId").value;
  var answerText = document.getElementById("answerText").value;
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
  } else {
    alert("WebSocket이 연결되지 않았습니다.");
  }
};

window.submitInterviewerFeedback = function(answerId) {
  var scoreInput = document.getElementById("score-" + answerId);
  var feedbackTextarea = document.getElementById("feedback-" + answerId);
  var evaluationDiv = document.getElementById("evaluation-" + answerId);
  
  if (!scoreInput || !feedbackTextarea) {
    console.error("입력 필드를 찾을 수 없습니다");
    return;
  }
  
  var score = scoreInput.value;
  var feedbackText = feedbackTextarea.value;
  
  if (!feedbackText.trim()) {
    alert('피드백을 입력해주세요.');
    return;
  }
  
  var submitBtn = evaluationDiv ? evaluationDiv.querySelector(".evaluation-submit-btn") : null;
  if (submitBtn && submitBtn.disabled) {
    console.log("이미 제출 중입니다");
    return;
  }
  
  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.textContent = '제출 중...';
  }
  
  if (window.mockerViewWS && window.mockerViewWS.connected) {
    window.mockerViewWS.submitInterviewerFeedback(answerId, parseInt(score), feedbackText);
    
    setTimeout(function() {
      if (evaluationDiv) {
        evaluationDiv.innerHTML = '<div class="submitted-evaluation">✅ 평가 제출 완료 (점수: ' + score + '/100)</div>';
      }
    }, 300);
  }
};

window.resumeInterview = function() {
  if (window.mockerViewWS && window.mockerViewWS.connected) {
    window.mockerViewWS.sendControlMessage('RESUME');
    document.getElementById('pauseInterviewBtn').textContent = '일시정지';
    document.getElementById('pauseInterviewBtn').onclick = window.pauseInterview;
  }
};

window.endInterview = function() {
  if (confirm('면접을 종료하시겠습니까?\n종료 후에는 답변을 제출할 수 없습니다.')) {
    if (window.mockerViewWS && window.mockerViewWS.connected) {
      window.mockerViewWS.sendControlMessage('END');
      document.getElementById('pauseInterviewBtn').style.display = 'none';
      document.getElementById('endInterviewBtn').style.display = 'none';
      alert('면접이 종료되었습니다.');
      setTimeout(function() {
        window.location.href = "/session/detail/" + SESSION_DATA.sessionId;
      }, 2000);
    }
  }
};