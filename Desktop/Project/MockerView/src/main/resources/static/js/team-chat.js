class TeamChatManager {
    constructor(sessionId, stompClient) {
        this.sessionId = sessionId;
        this.stompClient = stompClient;
        this.unreadCount = 0;
        this.initializeUI();
    }

    initializeUI() {
        const sendBtn = document.getElementById('send-chat-btn');
        const chatInput = document.getElementById('chat-input');

        if (sendBtn) {
            sendBtn.addEventListener('click', () => this.sendMessage());
        }

        if (chatInput) {
            chatInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    this.sendMessage();
                }
            });
        }

        this.stompClient.subscribe(`/topic/session/${this.sessionId}/chat`, (message) => {
            const chatMessage = JSON.parse(message.body);
            this.displayMessage(chatMessage);
            
            const container = document.getElementById('chat-container');
            if (container && container.classList.contains('minimized')) {
                this.unreadCount++;
                this.updateUnreadBadge();
            }
        });

        console.log('✅ 채팅 초기화 완료');
    }

    sendMessage() {
        const input = document.getElementById('chat-input');
        const message = input.value.trim();

        if (!message) return;

        this.stompClient.send(`/app/session/${this.sessionId}/chat`, {}, JSON.stringify({
            message: message
        }));

        input.value = '';
    }

    displayMessage(chatMessage) {
        const messagesDiv = document.getElementById('chat-messages');
        if (!messagesDiv) return;

        const messageEl = document.createElement('div');
        messageEl.className = 'chat-message';
        messageEl.innerHTML = `
            <div class="chat-message-header">
                <span class="chat-user">${chatMessage.userName}</span>
                <span class="chat-time">${this.formatTime(chatMessage.timestamp)}</span>
            </div>
            <div class="chat-message-body">${chatMessage.message}</div>
        `;

        messagesDiv.appendChild(messageEl);
        messagesDiv.scrollTop = messagesDiv.scrollHeight;
    }

    updateUnreadBadge() {
        const badge = document.getElementById('chat-unread-badge');
        if (badge) {
            badge.style.display = 'inline-block';
            badge.textContent = this.unreadCount;
        }
    }

    formatTime(timestamp) {
        const date = new Date(timestamp);
        return date.toLocaleTimeString('ko-KR', { 
            hour: '2-digit', 
            minute: '2-digit' 
        });
    }
}
