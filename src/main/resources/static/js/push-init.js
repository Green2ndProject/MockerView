document.addEventListener('DOMContentLoaded', function() {
    if ('serviceWorker' in navigator && 'PushManager' in window) {
        initPushNotification();
    }
});

function initPushNotification() {
    navigator.serviceWorker.register('/service-worker.js')
        .then(registration => {
            console.log('✅ Service Worker registered:', registration);
            
            return navigator.serviceWorker.ready;
        })
        .then(registration => {
            return registration.pushManager.getSubscription();
        })
        .then(subscription => {
            if (!subscription && Notification.permission === 'default') {
                showPushPrompt();
            } else if (subscription) {
                console.log('✅ Already subscribed to push');
            }
        })
        .catch(error => {
            console.error('❌ Service Worker registration failed:', error);
        });
}

function showPushPrompt() {
    const promptHtml = `
        <div id="push-prompt" style="
            position: fixed;
            bottom: 20px;
            right: 20px;
            background: white;
            border-radius: 12px;
            padding: 20px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            max-width: 320px;
            z-index: 10000;
            animation: slideIn 0.3s ease-out;
        ">
            <div style="display: flex; align-items: start; gap: 12px;">
                <div style="
                    width: 40px;
                    height: 40px;
                    background: #6366f1;
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    flex-shrink: 0;
                ">
                    <svg width="20" height="20" fill="white" viewBox="0 0 24 24">
                        <path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"/>
                    </svg>
                </div>
                <div style="flex: 1;">
                    <h4 style="margin: 0 0 8px 0; font-size: 16px; font-weight: 600; color: #111827;">
                        알림 받기
                    </h4>
                    <p style="margin: 0 0 16px 0; font-size: 14px; color: #6b7280; line-height: 1.5;">
                        세션 시작, 피드백 도착 등 중요한 알림을 받아보세요
                    </p>
                    <div style="display: flex; gap: 8px;">
                        <button onclick="enablePushNotifications()" style="
                            flex: 1;
                            background: #6366f1;
                            color: white;
                            border: none;
                            border-radius: 6px;
                            padding: 8px 16px;
                            font-size: 14px;
                            font-weight: 500;
                            cursor: pointer;
                        ">
                            허용
                        </button>
                        <button onclick="closePushPrompt()" style="
                            background: #f3f4f6;
                            color: #6b7280;
                            border: none;
                            border-radius: 6px;
                            padding: 8px 16px;
                            font-size: 14px;
                            font-weight: 500;
                            cursor: pointer;
                        ">
                            나중에
                        </button>
                    </div>
                </div>
            </div>
        </div>
        <style>
            @keyframes slideIn {
                from {
                    transform: translateY(20px);
                    opacity: 0;
                }
                to {
                    transform: translateY(0);
                    opacity: 1;
                }
            }
        </style>
    `;
    
    document.body.insertAdjacentHTML('beforeend', promptHtml);
}

function enablePushNotifications() {
    if (window.pushNotifications) {
        window.pushNotifications.requestPermission()
            .then(granted => {
                if (granted) {
                    showSuccessMessage('✅ 알림이 활성화되었습니다!');
                    closePushPrompt();
                } else {
                    showErrorMessage('❌ 알림 권한이 거부되었습니다');
                }
            })
            .catch(error => {
                console.error('Push permission error:', error);
                showErrorMessage('❌ 알림 설정 중 오류가 발생했습니다');
            });
    }
}

function closePushPrompt() {
    const prompt = document.getElementById('push-prompt');
    if (prompt) {
        prompt.style.animation = 'slideOut 0.3s ease-out';
        setTimeout(() => prompt.remove(), 300);
    }
}

function showSuccessMessage(message) {
    showToast(message, '#10b981');
}

function showErrorMessage(message) {
    showToast(message, '#ef4444');
}

function showToast(message, color) {
    const toast = document.createElement('div');
    toast.style.cssText = `
        position: fixed;
        bottom: 20px;
        right: 20px;
        background: ${color};
        color: white;
        padding: 12px 20px;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        z-index: 10001;
        animation: slideIn 0.3s ease-out;
    `;
    toast.textContent = message;
    document.body.appendChild(toast);
    
    setTimeout(() => {
        toast.style.animation = 'slideOut 0.3s ease-out';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}
