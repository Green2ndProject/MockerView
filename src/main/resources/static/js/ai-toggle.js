let currentSessionId = null;
let stompClient = null;

function toggleAI(enabled) {
    fetch(`/api/session/${currentSessionId}/ai/toggle`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({enabled: enabled})
    })
    .then(response => response.json())
    .then(data => {
        updateAIStatus(enabled);
        showToast(enabled ? 'AI 피드백이 활성화되었습니다' : 'AI 피드백이 비활성화되었습니다');
        document.getElementById('aiModeSection').style.display = enabled ? 'block' : 'none';
    })
    .catch(error => {
        console.error('AI 토글 실패:', error);
        document.getElementById('aiToggle').checked = !enabled;
    });
}

function changeAiMode(mode) {
    fetch(`/api/session/${currentSessionId}/ai/mode`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({mode: mode})
    })
    .then(response => response.json())
    .then(data => {
        showToast('AI 모드가 변경되었습니다');
    })
    .catch(error => console.error('AI 모드 변경 실패:', error));
}

function updateAIStatus(enabled) {
    const badge = document.getElementById('aiStatusText');
    if (enabled) {
        badge.className = 'badge bg-success';
        badge.innerHTML = '<i class="bi bi-robot"></i> AI 활성';
    } else {
        badge.className = 'badge bg-secondary';
        badge.innerHTML = '<i class="bi bi-robot"></i> AI 비활성';
    }
}

function subscribeToWebSocket() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    
    stompClient.connect({}, function(frame) {
        stompClient.subscribe(`/topic/session/${currentSessionId}`, function(message) {
            const data = JSON.parse(message.body);
            
            switch(data.type) {
                case 'AI_TOGGLE':
                    handleAIToggle(data);
                    break;
                case 'AI_MODE_CHANGE':
                    handleAIModeChange(data);
                    break;
                case 'AI_FEEDBACK_READY':
                    handleAIFeedbackReady(data);
                    break;
            }
        });
    });
}

function handleAIToggle(data) {
    document.getElementById('aiToggle').checked = data.enabled;
    updateAIStatus(data.enabled);
    showToast(data.message + ' (변경: ' + data.changedBy + ')');
}

function handleAIModeChange(data) {
    document.getElementById('aiMode').value = data.mode;
    showToast(data.message);
}

function handleAIFeedbackReady(data) {
    const answerElement = document.getElementById('answer-' + data.answerId);
    const feedbackContainer = answerElement.querySelector('.ai-feedback-container');
    
    feedbackContainer.innerHTML = `
        <div class="alert alert-info">
            <strong><i class="bi bi-robot"></i> AI 피드백</strong>
            <p>${data.feedback.replace(/\n/g, '<br>')}</p>
            <small class="text-muted">처리 시간: ${data.processingTime}ms</small>
        </div>
        <button class="btn btn-sm btn-outline-secondary" onclick="regenerateAiFeedback(${data.answerId})">
            <i class="bi bi-arrow-clockwise"></i> 다시 생성
        </button>
    `;
}

function showToast(message) {
    const toast = document.createElement('div');
    toast.className = 'toast-notification';
    toast.textContent = message;
    document.body.appendChild(toast);
    
    setTimeout(() => toast.classList.add('show'), 100);
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

document.addEventListener('DOMContentLoaded', function() {
    currentSessionId = document.getElementById('sessionId').value;
    subscribeToWebSocket();
});
