let deferredPrompt;
let installShown = false;

window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault();
    deferredPrompt = e;
    
    if (!localStorage.getItem('pwa-install-dismissed') && isMobile()) {
        setTimeout(() => showInstallPrompt(), 3000);
    }
});

function isMobile() {
    return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
}

function isIOS() {
    return /iPhone|iPad|iPod/.test(navigator.userAgent);
}

function isStandalone() {
    return window.matchMedia('(display-mode: standalone)').matches || 
           window.navigator.standalone === true;
}

function showInstallPrompt() {
    if (installShown || isStandalone()) return;
    installShown = true;
    
    const isIOSDevice = isIOS();
    
    const promptHTML = `
        <div id="pwa-install-prompt" style="
            position: fixed;
            bottom: 0;
            left: 0;
            right: 0;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 20px;
            box-shadow: 0 -4px 20px rgba(0,0,0,0.2);
            z-index: 10000;
            animation: slideUp 0.3s ease-out;
        ">
            <div style="max-width: 500px; margin: 0 auto;">
                <div style="display: flex; align-items: start; gap: 16px;">
                    <div style="
                        width: 48px;
                        height: 48px;
                        background: white;
                        border-radius: 12px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        flex-shrink: 0;
                    ">
                        <img src="/images/192.png" style="width: 40px; height: 40px; border-radius: 8px;">
                    </div>
                    <div style="flex: 1;">
                        <h3 style="margin: 0 0 8px 0; font-size: 18px; font-weight: 600;">
                            MockerView 앱 설치
                        </h3>
                        <p style="margin: 0 0 16px 0; font-size: 14px; opacity: 0.9; line-height: 1.5;">
                            ${isIOSDevice 
                                ? '홈 화면에 추가하여 앱처럼 사용하세요!' 
                                : '홈 화면에 추가하고 빠르게 접속하세요!'}
                        </p>
                        ${isIOSDevice ? `
                            <div style="background: rgba(255,255,255,0.2); padding: 12px; border-radius: 8px; margin-bottom: 16px; font-size: 13px;">
                                <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px;">
                                    <span style="font-size: 20px;">⬆️</span>
                                    <span>1. 하단 공유 버튼 탭</span>
                                </div>
                                <div style="display: flex; align-items: center; gap: 8px;">
                                    <span style="font-size: 20px;">➕</span>
                                    <span>2. "홈 화면에 추가" 선택</span>
                                </div>
                            </div>
                        ` : ''}
                        <div style="display: flex; gap: 12px;">
                            ${!isIOSDevice ? `
                                <button onclick="installPWA()" style="
                                    flex: 1;
                                    background: white;
                                    color: #667eea;
                                    border: none;
                                    border-radius: 8px;
                                    padding: 12px;
                                    font-size: 15px;
                                    font-weight: 600;
                                    cursor: pointer;
                                ">
                                    설치하기
                                </button>
                            ` : ''}
                            <button onclick="dismissInstallPrompt()" style="
                                ${isIOSDevice ? 'flex: 1;' : ''}
                                background: rgba(255,255,255,0.2);
                                color: white;
                                border: none;
                                border-radius: 8px;
                                padding: 12px ${isIOSDevice ? '' : '20px'};
                                font-size: 15px;
                                font-weight: 600;
                                cursor: pointer;
                            ">
                                ${isIOSDevice ? '확인' : '나중에'}
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <style>
            @keyframes slideUp {
                from {
                    transform: translateY(100%);
                    opacity: 0;
                }
                to {
                    transform: translateY(0);
                    opacity: 1;
                }
            }
        </style>
    `;
    
    document.body.insertAdjacentHTML('beforeend', promptHTML);
}

async function installPWA() {
    if (!deferredPrompt) return;
    
    deferredPrompt.prompt();
    const { outcome } = await deferredPrompt.userChoice;
    
    if (outcome === 'accepted') {
        console.log('✅ PWA installed');
        showToast('✅ 앱이 설치되었습니다!', '#10b981');
    } else {
        console.log('❌ PWA installation dismissed');
    }
    
    deferredPrompt = null;
    dismissInstallPrompt();
}

function dismissInstallPrompt() {
    const prompt = document.getElementById('pwa-install-prompt');
    if (prompt) {
        prompt.style.animation = 'slideDown 0.3s ease-out';
        setTimeout(() => prompt.remove(), 300);
    }
    localStorage.setItem('pwa-install-dismissed', 'true');
}

function showToast(message, color) {
    const toast = document.createElement('div');
    toast.style.cssText = `
        position: fixed;
        bottom: 20px;
        left: 50%;
        transform: translateX(-50%);
        background: ${color};
        color: white;
        padding: 12px 24px;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        z-index: 10001;
        animation: slideUp 0.3s ease-out;
        font-weight: 500;
    `;
    toast.textContent = message;
    document.body.appendChild(toast);
    
    setTimeout(() => {
        toast.style.animation = 'slideDown 0.3s ease-out';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

window.addEventListener('appinstalled', () => {
    console.log('✅ PWA successfully installed');
    showToast('✅ 앱 설치 완료!', '#10b981');
    localStorage.removeItem('pwa-install-dismissed');
});
