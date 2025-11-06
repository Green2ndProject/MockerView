function checkSessionLimitBeforeCreate() {
    console.log('🔍 세션 제한 체크 시작...');
    fetch('/api/session-limit/check', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => {
        console.log('📡 응답 상태:', response.status);
        return response.json();
    })
    .then(data => {
        console.log('📦 받은 데이터:', data);
        console.log('🚨 제한 도달 여부:', data.limitReached);
        
        if (data.limitReached) {
            console.log('⚠️ 제한 도달! 모달 표시');
            showUpgradeModal(data);
        } else {
            console.log('✅ 제한 OK! 세션 모달 표시');
            document.getElementById('sessionModal').style.display = 'flex';
        }
    })
    .catch(error => {
        console.error('❌ 세션 제한 체크 실패:', error);
        alert('세션 생성 가능 여부를 확인할 수 없습니다.');
    });
}

function showUpgradeModal(limitInfo) {
    console.log('🎨 모달 생성 시작...', limitInfo);
    const modal = document.createElement('div');
    modal.id = 'upgradeModal';
    modal.className = 'modal-overlay';
    modal.innerHTML = `
        <div class="upgrade-modal">
            <div class="modal-header">
                <h2>세션 생성 한도 도달</h2>
                <button class="close-btn" onclick="closeUpgradeModal()">&times;</button>
            </div>
            <div class="modal-body">
                <div class="limit-icon">⚠️</div>
                <p class="limit-message">${limitInfo.message || '세션 생성 한도에 도달했습니다.'}</p>
                <div class="usage-info">
                    <div class="usage-bar-container">
                        <div class="usage-bar" style="width: ${(limitInfo.usedSessions / limitInfo.sessionLimit * 100)}%"></div>
                    </div>
                    <p class="usage-text">
                        <strong>${limitInfo.usedSessions}</strong> / ${limitInfo.sessionLimit} 세션 사용 완료
                    </p>
                </div>
                <div class="plan-info">
                    <p>현재 플랜: <strong>${getPlanDisplayName(limitInfo.currentPlan)}</strong></p>
                </div>
                <p class="upgrade-description">
                    더 많은 세션을 생성하려면 플랜을 업그레이드하세요.
                </p>
            </div>
            <div class="modal-footer">
                <button class="btn-cancel" onclick="closeUpgradeModal()">취소</button>
                <button class="btn-upgrade" onclick="goToUpgrade()">플랜 업그레이드</button>
            </div>
        </div>
    `;
    
    document.body.appendChild(modal);
    console.log('✅ 모달 DOM 추가 완료');
    
    setTimeout(() => {
        modal.classList.add('active');
        console.log('✅ 모달 active 클래스 추가');
    }, 10);
}

function closeUpgradeModal() {
    const modal = document.getElementById('upgradeModal');
    if (modal) {
        modal.classList.remove('active');
        setTimeout(() => {
            modal.remove();
        }, 300);
    }
}

function goToUpgrade() {
    window.location.href = '/payment/plans';
}

function getPlanDisplayName(planType) {
    const planNames = {
        'FREE': '무료 플랜',
        'BASIC': '베이직 플랜',
        'PRO': '프로 플랜',
        'TEAM': '팀 플랜',
        'ENTERPRISE': '엔터프라이즈',
        'NONE': '플랜 없음'
    };
    return planNames[planType] || planType;
}

document.addEventListener('DOMContentLoaded', function() {
    const urlParams = new URLSearchParams(window.location.search);
    const errorCode = urlParams.get('error');
    
    if (errorCode === 'SESSION_LIMIT_EXCEEDED') {
        fetch('/api/session-limit/check')
            .then(response => response.json())
            .then(data => {
                showUpgradeModal(data);
            })
            .catch(error => {
                console.error('세션 제한 정보 로드 실패:', error);
                alert('세션 생성 한도에 도달했습니다. 플랜을 업그레이드해주세요.');
            });
    }
});