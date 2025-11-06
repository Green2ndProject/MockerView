class NotificationHandler {
    constructor(userId, options = {}) {
        this.userId = userId;
        this.stompClient = null;
        this.notifications = [];
        this.isSelfInterview = options.isSelfInterview || false;
        this.enableToast = options.enableToast !== false;
        
        if (!this.isSelfInterview && this.enableToast) {
            this.initWebSocket();
            this.createNotificationContainer();
        } else {
            console.log('🔕 알림 비활성화 (셀프 면접 모드)');
        }
    }

    initWebSocket() {
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);
        
        this.stompClient.connect({}, () => {
            console.log('✅ 알림 WebSocket 연결됨');
            
            this.stompClient.subscribe('/topic/user/' + this.userId + '/notifications', (message) => {
                const notification = JSON.parse(message.body);
                if (this.enableToast && !this.isSelfInterview) {
                    this.showNotification(notification);
                }
            });
            
            this.stompClient.subscribe('/topic/user/' + this.userId + '/analysis', (message) => {
                const analysisComplete = JSON.parse(message.body);
                console.log('✅ 분석 완료:', analysisComplete);
            });
        }, (error) => {
            console.error('❌ WebSocket 연결 실패:', error);
        });
    }

    createNotificationContainer() {
        if (document.getElementById('notification-container')) return;
        
        const container = document.createElement('div');
        container.id = 'notification-container';
        container.style.cssText = `
            position: fixed;
            top: 80px;
            right: 20px;
            z-index: 9999;
            display: flex;
            flex-direction: column;
            gap: 10px;
            max-width: 350px;
        `;
        document.body.appendChild(container);
    }

    showNotification(notification) {
        if (this.isSelfInterview || !this.enableToast) {
            console.log('🔕 알림 스킵 (셀프 면접 모드)');
            return;
        }
        
        const container = document.getElementById('notification-container');
        if (!container) return;
        
        const notifElement = document.createElement('div');
        notifElement.className = 'notification-item';
        notifElement.style.cssText = `
            background: white;
            border-radius: 12px;
            padding: 16px 20px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            border-left: 4px solid #667eea;
            cursor: pointer;
            animation: slideIn 0.3s ease;
            transition: all 0.2s;
        `;
        
        notifElement.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: start; margin-bottom: 8px;">
                <strong style="color: #1f2937; font-size: 0.9375rem;">${notification.title}</strong>
                <button onclick="this.parentElement.parentElement.remove()" 
                        style="background: none; border: none; color: #9ca3af; cursor: pointer; font-size: 1.25rem; padding: 0; line-height: 1;">
                    ×
                </button>
            </div>
            <p style="color: #6b7280; margin: 0; font-size: 0.875rem; line-height: 1.4;">${notification.message}</p>
        `;
        
        notifElement.onmouseenter = () => {
            notifElement.style.transform = 'translateY(-2px)';
            notifElement.style.boxShadow = '0 6px 16px rgba(0,0,0,0.2)';
        };
        
        notifElement.onmouseleave = () => {
            notifElement.style.transform = 'translateY(0)';
            notifElement.style.boxShadow = '0 4px 12px rgba(0,0,0,0.15)';
        };
        
        if (notification.link) {
            notifElement.onclick = () => {
                window.location.href = notification.link;
            };
        }
        
        container.appendChild(notifElement);
        
        setTimeout(() => {
            notifElement.style.animation = 'slideOut 0.3s ease';
            setTimeout(() => notifElement.remove(), 300);
        }, 5000);
        
        this.playNotificationSound();
    }

    playNotificationSound() {
        if (this.isSelfInterview || !this.enableToast) return;
        
        try {
            const audio = new Audio('data:audio/wav;base64,UklGRnoGAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQoGAACBhYqFbF1fdJivrJBhNjVgodDbq2EcBj+a2/LDciUFLIHO8tiJNwgZaLvt559NEAxQp+PwtmMcBjiR1/LMeSwFJHfH8N2QQAoUXrTp66hVFApGn+DyvmwhBSuBzvLZiTYIGGe77eeeTRAMUKfj8LZjHAU7k9nyz3ssBSx+zPDckkILFGCz6OyrWBUIR6Hh87xrIAUsgs/y2ok2CBhnu+3nnk0QDFC747C2YxwFO5PZ8s97LAUsfs/w3ZJCCxRgs+jsq1kVCEeh4fO8ayAFLILP8tqJNggYZ7vt559NEAxQu+Owtm');
            audio.volume = 0.3;
            audio.play().catch(e => console.log('알림음 재생 실패:', e));
        } catch (e) {
            console.log('알림음 재생 실패:', e);
        }
    }
    
    disconnect() {
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.disconnect(() => {
                console.log('✅ 알림 WebSocket 연결 해제됨');
            });
        }
    }
}

const notificationCSS = document.createElement('style');
notificationCSS.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(400px);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    @keyframes slideOut {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(400px);
            opacity: 0;
        }
    }
`;
document.head.appendChild(notificationCSS);
