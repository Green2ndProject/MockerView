class MockerViewWebSocket {
  constructor(sessionId, userId, userName) {
    this.sessionId = sessionId;
    this.userId = userId;
    this.userName = userName;
    this.stompClient = null;
    this.connected = false;
  }

  connect() {
    const socket = new SockJS("/ws");
    this.stompClient = Stomp.over(socket);

    this.stompClient.connect(
      {},
      (frame) => {
        console.log("Connected: " + frame);
        this.connected = true;

        this.subscribeToTopics();
        this.joinSession();
      },
      (error) => {
        console.error("Connection error: ", error);
      }
    );
  }

  subscribeToTopics() {
    this.stompClient.subscribe(
      `/topic/session/${this.sessionId}/status`,
      (message) => {
        this.handleStatusUpdate(JSON.parse(message.body));
      }
    );

    this.stompClient.subscribe(
      `/topic/session/${this.sessionId}/question`,
      (message) => {
        this.handleNewQuestion(JSON.parse(message.body));
      }
    );

    this.stompClient.subscribe(
      `/topic/session/${this.sessionId}/answer`,
      (message) => {
        this.handleNewAnswer(JSON.parse(message.body));
      }
    );

    this.stompClient.subscribe(
      `/topic/session/${this.sessionId}/feedback`,
      (message) => {
        this.handleNewFeedback(JSON.parse(message.body));
      }
    );

    this.stompClient.subscribe(
      `/topic/session/${this.sessionId}/interviewer-feedback`,
      (message) => {
        this.handleInterviewerFeedback(JSON.parse(message.body));
      }
    );
  }

  joinSession() {
    if (this.connected) {
      this.stompClient.send(
        `/app/session/${this.sessionId}/join`,
        {},
        JSON.stringify({
          sessionId: this.sessionId,
          userId: this.userId,
          userName: this.userName,
          action: "JOIN",
        })
      );
    }
  }

  leaveSession() {
    if (this.connected) {
      this.stompClient.send(
        `/app/session/${this.sessionId}/leave`,
        {},
        JSON.stringify({
          sessionId: this.sessionId,
          userId: this.userId,
          userName: this.userName,
          action: "LEAVE",
        })
      );
    }
  }

  sendQuestion(questionText, orderNo, timer) {
    if (this.connected) {
      this.stompClient.send(
        `/app/session/${this.sessionId}/question`,
        {},
        JSON.stringify({
          questionText: questionText,
          orderNo: orderNo,
          timer: parseInt(timer) || 30,
        })
      );
    }
  }

  submitAnswer(questionId, answerText) {
    if (this.connected) {
      this.stompClient.send(
        `/app/session/${this.sessionId}/answer`,
        {},
        JSON.stringify({
          questionId: parseInt(questionId),
          userId: this.userId,
          userName: this.userName,
          answerText: answerText,
        })
      );
    }
  }

  submitInterviewerFeedback(answerId, score, comment) {
    if (this.connected) {
      this.stompClient.send(
        `/app/session/${this.sessionId}/interviewer-feedback`,
        {},
        JSON.stringify({
          answerId: answerId,
          reviewerId: this.userId,
          reviewerName: this.userName,
          score: score,
          comment: comment,
        })
      );
    }
  }

  getSessionStatus() {
    if (this.connected) {
      this.stompClient.send(
        `/app/session/${this.sessionId}/status`,
        {},
        JSON.stringify({
          sessionId: this.sessionId,
        })
      );
    }
  }

  handleStatusUpdate(message) {
    console.log("Status update:", message);

    if (message.participants) {
      this.updateParticipantsList(message.participants);
    }

    if (
      message.questionCount !== undefined &&
      message.answerCount !== undefined
    ) {
      this.updateSessionStats(message.questionCount, message.answerCount);
    }

    if (message.action === "JOIN") {
      this.showNotification(`${message.userName}님이 참가했습니다.`);
    } else if (message.action === "LEAVE") {
      this.showNotification(`${message.userName}님이 퇴장했습니다.`);
    }
  }

  handleNewQuestion(message) {
    console.log("New question:", message);
    this.displayQuestion(message.questionText, message.orderNo, message.timer);

    const questionIdInput = document.getElementById("currentQuestionId");
    if (questionIdInput) {
      questionIdInput.value = message.questionId || "";
    }
  }

  handleNewAnswer(message) {
    console.log("New answer:", message);
    this.displayAnswer(message);
    this.showNotification(`${message.userName}님이 답변을 제출했습니다.`);
  }

  handleNewFeedback(message) {
    console.log("New feedback:", message);
    this.displayAIFeedback(message);
    this.showNotification("AI 피드백이 생성되었습니다.");
  }

  handleInterviewerFeedback(message) {
    console.log("Interviewer feedback:", message);
    this.displayInterviewerFeedback(message);
    this.showNotification(
      `${message.reviewerName}님이 면접관 피드백을 주었습니다.`
    );
  }

  updateParticipantsList(participants) {
    const participantsList = document.getElementById("participants-list");
    if (participantsList && participants && Array.isArray(participants)) {
      participantsList.innerHTML = participants
        .map((name) => `<li>${name}</li>`)
        .join("");
    }
  }

  updateSessionStats(questionCount, answerCount) {
    const statsDiv = document.getElementById("session-stats");
    if (statsDiv) {
      statsDiv.innerHTML = `질문: ${questionCount || 0}개 | 답변: ${
        answerCount || 0
      }개`;
    }
  }

  displayQuestion(questionText, orderNo, timer) {
    const questionTextElement = document.getElementById(
      "current-question-text"
    );
    if (questionTextElement) {
      questionTextElement.textContent = questionText;
    }
    const sessionTimerElement = document.getElementById("session-timer");
    if (sessionTimerElement && timer !== undefined && timer !== null) {
      const totalSeconds = parseInt(timer, 10);

      const minutes = Math.floor(totalSeconds / 60)
        .toString()
        .padStart(2, "0");
      const seconds = (totalSeconds % 60).toString().padStart(2, "0");

      sessionTimerElement.textContent = `${minutes}:${seconds}`;
      console.log(`[DEBUG] 타이머 화면 갱신: ${minutes}:${seconds}`);
    }

    const questionDiv = document.getElementById("current-question");
    if (questionDiv) {
      questionDiv.className = "current-question active-question";
    }
  }

  displayAnswer(answer) {
    const answersDiv = document.getElementById("answers-list");
    if (answersDiv) {
      const answerElement = document.createElement("div");
      answerElement.className = "answer-item";
      answerElement.id = `answer-${answer.answerId}`;
      answerElement.innerHTML = `
                <div class="answer-header">
                    <div class="answer-user">${answer.userName}</div>
                    <div class="answer-time">${new Date().toLocaleTimeString()}</div>
                </div>
                <div class="answer-text">${answer.answerText}</div>
                <div class="ai-feedback-placeholder">
                    <div class="loading">
                        <div class="spinner"></div>
                        AI 피드백 생성 중...
                    </div>
                </div>
                ${
                  this.getUserRole() === "HOST" ||
                  this.getUserRole() === "REVIEWER"
                    ? this.getInterviewerFeedbackForm(answer.answerId)
                    : ""
                }
            `;
      answersDiv.appendChild(answerElement);

      if (this.getUserRole() === "HOST" || this.getUserRole() === "REVIEWER") {
        this.attachFeedbackFormHandlers(answer.answerId);
      }
    }
  }

  getInterviewerFeedbackForm(answerId) {
    return `
            <div class="feedback-form">
                <div class="feedback-form-title">면접관 피드백</div>
                <div class="row">
                    <select class="score-input">
                        <option value="">점수 선택</option>
                        ${Array.from(
                          { length: 10 },
                          (_, i) =>
                            `<option value="${i + 1}">${i + 1}점</option>`
                        ).join("")}
                    </select>
                    <button class="btn btn-warning submit-feedback-btn" data-answer-id="${answerId}">제출</button>
                </div>
                <textarea class="comment-input" placeholder="피드백을 입력하세요..."></textarea>
            </div>
        `;
  }

  attachFeedbackFormHandlers(answerId) {
    const answerElement = document.getElementById(`answer-${answerId}`);
    const submitBtn = answerElement.querySelector(".submit-feedback-btn");

    if (submitBtn) {
      submitBtn.addEventListener("click", () => {
        const scoreInput = answerElement.querySelector(".score-input");
        const commentInput = answerElement.querySelector(".comment-input");

        const score = parseInt(scoreInput.value);
        const comment = commentInput.value.trim();

        if (!score || score < 1 || score > 10) {
          alert("점수를 1-10 사이로 선택해주세요.");
          return;
        }

        if (!comment) {
          alert("코멘트를 입력해주세요.");
          return;
        }

        this.submitInterviewerFeedback(answerId, score, comment);

        scoreInput.value = "";
        commentInput.value = "";

        const feedbackForm = answerElement.querySelector(".feedback-form");
        feedbackForm.innerHTML =
          '<div class="text-success">피드백이 제출되었습니다.</div>';
      });
    }
  }

  displayAIFeedback(feedback) {
    const answerElement = document.getElementById(
      `answer-${feedback.answerId}`
    );
    if (answerElement) {
      const placeholder = answerElement.querySelector(
        ".ai-feedback-placeholder"
      );
      if (placeholder) {
        placeholder.innerHTML = `
                    <div class="ai-feedback">
                        <div class="ai-feedback-title">🤖 AI 피드백</div>
                        <div class="feedback-content">
                            <div class="feedback-section">
                                <strong>📋 요약:</strong>
                                <p>${feedback.summary}</p>
                            </div>
                            <div class="feedback-section">
                                <strong class="text-success">✅ 강점:</strong>
                                <p>${feedback.strengths}</p>
                            </div>
                            <div class="feedback-section">
                                <strong class="text-warning">⚠️ 개선점:</strong>
                                <p>${feedback.weaknesses}</p>
                            </div>
                            <div class="feedback-section">
                                <strong class="text-info">💡 제안사항:</strong>
                                <p>${feedback.improvement}</p>
                            </div>
                            <div class="feedback-meta">
                                <small class="text-muted">
                                    ${
                                      feedback.model
                                    } | ${new Date().toLocaleTimeString()}
                                </small>
                            </div>
                        </div>
                    </div>
                `;
      }
    }
  }

  displayInterviewerFeedback(feedback) {
    const answerElement = document.getElementById(
      `answer-${feedback.answerId}`
    );
    if (answerElement) {
      const existingInterviewerFeedback = answerElement.querySelector(
        ".interviewer-feedback"
      );
      if (existingInterviewerFeedback) {
        existingInterviewerFeedback.remove();
      }

      const feedbackHtml = `
                <div class="interviewer-feedback">
                    <div class="interviewer-feedback-title">👨‍💼 면접관 피드백 - ${
                      feedback.reviewerName
                    }</div>
                    <div><strong>점수:</strong> <span class="score-badge">${
                      feedback.score
                    }/10</span></div>
                    <div><strong>코멘트:</strong> ${feedback.comment}</div>
                    <div class="text-muted">${new Date().toLocaleTimeString()}</div>
                </div>
            `;

      answerElement.insertAdjacentHTML("beforeend", feedbackHtml);
    }
  }

  getUserRole() {
    const userRoleInput = document.getElementById("userRole");
    return userRoleInput ? userRoleInput.value : "STUDENT";
  }

  showNotification(message) {
    const notification = document.createElement("div");
    notification.className = "notification";
    notification.textContent = message;

    const container = document.getElementById("toast-container");
    if (container) {
      container.appendChild(notification);

      setTimeout(() => {
        if (container.contains(notification)) {
          container.removeChild(notification);
        }
      }, 3000);
    }
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

  window.addEventListener("beforeunload", () => {
    if (mockerViewWS) {
      mockerViewWS.disconnect();
    }
  });
}

function submitAnswer() {
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

  if (mockerViewWS && mockerViewWS.connected) {
    mockerViewWS.submitAnswer(questionId, answerText);
    document.getElementById("answerText").value = "";
  } else {
    alert("WebSocket이 연결되지 않았습니다.");
  }
}

function sendQuestion() {
  const questionText = document.getElementById("newQuestionText").value;
  const orderNo = document.getElementById("newQuestionOrder").value;
  const timerOrder = document.getElementById("newTimerOrder").value;

  if (!questionText.trim()) {
    alert("질문을 입력해주세요.");
    return;
  }

  if (mockerViewWS && mockerViewWS.connected) {
    mockerViewWS.sendQuestion(
      questionText,
      parseInt(orderNo) || 1,
      parseInt(timerOrder) || 30
    );
    document.getElementById("newQuestionText").value = "";
    document.getElementById("newQuestionOrder").value = "";
    document.getElementById("newTimerOrder").value = "";
  } else {
    alert("WebSocket이 연결되지 않았습니다.");
  }
}
