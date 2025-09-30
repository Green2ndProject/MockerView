let stompClient = null;
let sessionId = null;
let userId = null;
let userName = null;
let userRole = null;

function initializeWebSocket(sessionIdParam, userIdParam, userNameParam) {
  sessionId = sessionIdParam;
  userId = userIdParam;
  userName = userNameParam;
  userRole = document.getElementById("userRole").value;

  connect();
}

function connect() {
  const socket = new SockJS("/ws");
  stompClient = Stomp.over(socket);

  stompClient.connect(
    {},
    function (frame) {
      console.log("Connected: " + frame);

      stompClient.subscribe(
        "/topic/session/" + sessionId + "/answer",
        function (message) {
          const answer = JSON.parse(message.body);
          displayAnswer(answer);
        }
      );

      stompClient.subscribe(
        "/topic/session/" + sessionId + "/question",
        function (message) {
          const question = JSON.parse(message.body);
          displayQuestion(question);
        }
      );

      stompClient.subscribe(
        "/topic/session/" + sessionId + "/feedback",
        function (message) {
          const feedback = JSON.parse(message.body);
          displayAIFeedback(feedback);
        }
      );

      stompClient.subscribe(
        "/topic/session/" + sessionId + "/interviewer-feedback",
        function (message) {
          const feedback = JSON.parse(message.body);
          displayInterviewerFeedback(feedback);
        }
      );

      stompClient.subscribe(
        "/topic/session/" + sessionId + "/status",
        function (message) {
          const status = JSON.parse(message.body);
          updateSessionStatus(status);
        }
      );

      console.log("WebSocket connected successfully");
    },
    function (error) {
      console.error("WebSocket connection error:", error);
      showNotification("WebSocket 연결에 실패했습니다");
    }
  );
}

function disconnect() {
  if (stompClient !== null) {
    stompClient.disconnect();
  }
  console.log("Disconnected");
}

function sendQuestion() {
  if (!stompClient || !stompClient.connected) {
    console.error("WebSocket not connected");
    showNotification("WebSocket이 연결되지 않았습니다");
    return;
  }

  const questionText = document.getElementById("newQuestionText").value;
  const questionOrder = document.getElementById("newQuestionOrder").value;
  // 이게 타이머 집어 넣는거
  const timerOrder = document.getElementById("newTimerOrder").value;

  if (!questionText.trim()) {
    alert("질문을 입력해주세요.");
    return;
  }

  const questionMessage = {
    questionText: questionText,
    orderNo: parseInt(questionOrder) || 1,
    timer: parseInt(timerOrder) || 30,
    timestamp: new Date().toISOString(),
  };

  console.log("Sending question:", questionMessage);
  stompClient.send(
    "/app/session/" + sessionId + "/question",
    {},
    JSON.stringify(questionMessage)
  );

  //이게 보내고 나서 비워주는겨?
  document.getElementById("newQuestionText").value = "";
  document.getElementById("newQuestionOrder").value = "";
  document.getElementById("newTimerOrder").value = "";
}

function submitAnswer() {
  if (!stompClient || !stompClient.connected) {
    console.error("WebSocket not connected");
    showNotification("WebSocket이 연결되지 않았습니다");
    return;
  }

  const answerText = document.getElementById("answerText").value;
  const questionId = document.getElementById("currentQuestionId").value;

  if (!answerText.trim()) {
    alert("답변을 입력해주세요.");
    return;
  }

  if (!questionId) {
    alert("현재 답변할 질문이 없습니다.");
    return;
  }

  const answerMessage = {
    questionId: parseInt(questionId),
    userId: parseInt(userId),
    userName: userName,
    answerText: answerText,
    timestamp: new Date().toISOString(),
  };

  console.log("Submitting answer:", answerMessage);
  stompClient.send(
    "/app/session/" + sessionId + "/answer",
    {},
    JSON.stringify(answerMessage)
  );

  document.getElementById("answerText").value = "";

  // AI 피드백 대기 표시
  showAIFeedbackWaiting();
}

function showAIFeedbackWaiting() {
  const aiFeedbackList = document.getElementById("ai-feedback-list");

  const waitingElement = document.createElement("div");
  waitingElement.className = "ai-feedback-waiting";
  waitingElement.innerHTML = `
        <div class="loading">
            <div class="spinner"></div>
            AI가 답변을 분석하고 있습니다...
        </div>
    `;

  aiFeedbackList.appendChild(waitingElement);
}

function displayQuestion(question) {
  document.getElementById("current-question-text").textContent =
    question.questionText;
  document.getElementById("currentQuestionId").value = question.questionId;
  document.getElementById("session-timer").textContent = question.timer;
}

function displayAnswer(answer) {
  const answersDiv = document.getElementById("answers-list");

  const answerElement = document.createElement("div");
  answerElement.className = "answer-item";
  answerElement.id = "answer-" + answer.answerId;

  answerElement.innerHTML = `
        <div class="answer-header">
            <div class="answer-user">${answer.userName}</div>
            <div class="answer-time">${formatDateTime(answer.timestamp)}</div>
        </div>
        <div class="answer-text">${answer.answerText}</div>
        ${getInterviewerFeedbackForm(answer.answerId)}
    `;

  answersDiv.appendChild(answerElement);

  if (isInterviewer()) {
    setupFeedbackForm(answer.answerId);
  }
}

function getInterviewerFeedbackForm(answerId) {
  if (!isInterviewer()) {
    return "";
  }

  return `
        <div class="feedback-form" id="feedback-form-${answerId}">
            <div class="feedback-form-title">면접관 피드백</div>
            <div class="row">
                <select id="score-${answerId}">
                    <option value="">점수 선택</option>
                    <option value="1">1점</option>
                    <option value="2">2점</option>
                    <option value="3">3점</option>
                    <option value="4">4점</option>
                    <option value="5">5점</option>
                    <option value="6">6점</option>
                    <option value="7">7점</option>
                    <option value="8">8점</option>
                    <option value="9">9점</option>
                    <option value="10">10점</option>
                </select>
                <button class="btn btn-warning" onclick="submitFeedbackForAnswer(${answerId})">제출</button>
            </div>
            <textarea id="comment-${answerId}" placeholder="피드백을 입력하세요..."></textarea>
        </div>
    `;
}

function setupFeedbackForm(answerId) {}

function submitFeedbackForAnswer(answerId) {
  const score = document.getElementById(`score-${answerId}`).value;
  const comment = document.getElementById(`comment-${answerId}`).value;

  if (!score || score < 1 || score > 10) {
    alert("점수를 1-10 사이로 선택해주세요.");
    return;
  }

  if (!comment.trim()) {
    alert("코멘트를 입력해주세요.");
    return;
  }

  const feedbackMessage = {
    answerId: parseInt(answerId),
    reviewerId: parseInt(userId),
    reviewerName: userName,
    score: parseInt(score),
    comment: comment.trim(),
  };

  stompClient.send(
    "/app/session/" + sessionId + "/interviewer-feedback",
    {},
    JSON.stringify(feedbackMessage)
  );

  const feedbackForm = document.getElementById(`feedback-form-${answerId}`);
  feedbackForm.innerHTML =
    '<div class="text-success">피드백이 제출되었습니다.</div>';
}

function displayAIFeedback(feedback) {
  console.log("Displaying AI feedback:", feedback);

  const waitingElement = document.querySelector(".ai-feedback-waiting");
  if (waitingElement) {
    waitingElement.remove();
  }

  const aiFeedbackList = document.getElementById("ai-feedback-list");

  const feedbackElement = document.createElement("div");
  feedbackElement.className = "ai-feedback-item";
  feedbackElement.innerHTML = `
        <div class="ai-feedback">
            <div class="ai-feedback-title">🤖 AI 피드백 #${
              feedback.answerId
            }</div>
            <div class="feedback-content">
                <div class="feedback-section">
                    <strong>📝 요약:</strong>
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
                        ${feedback.model} | ${formatDateTime(
    feedback.timestamp
  )}
                    </small>
                </div>
            </div>
        </div>
    `;

  aiFeedbackList.appendChild(feedbackElement);

  feedbackElement.scrollIntoView({ behavior: "smooth" });
}

function displayInterviewerFeedback(feedback) {
  const answerElement = document.getElementById("answer-" + feedback.answerId);
  if (!answerElement) return;

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
            <div class="text-muted">${formatDateTime(feedback.timestamp)}</div>
        </div>
    `;

  answerElement.insertAdjacentHTML("beforeend", feedbackHtml);
}

function updateSessionStatus(status) {
  console.log("Session status updated:", status);
}

function isInterviewer() {
  return userRole === "HOST" || userRole === "REVIEWER";
}

function formatDateTime(timestamp) {
  return new Date(timestamp).toLocaleString("ko-KR");
}

function showNotification(message) {
  const notification = document.createElement("div");
  notification.className = "notification";
  notification.textContent = message;

  document.body.appendChild(notification);

  setTimeout(() => {
    if (document.body.contains(notification)) {
      document.body.removeChild(notification);
    }
  }, 3000);
}

document.addEventListener("DOMContentLoaded", function () {
  window.addEventListener("beforeunload", function () {
    disconnect();
  });
});
