let isCreatingSelfInterview = false;

function checkSessionLimitForSelfInterview() {
    if (isCreatingSelfInterview) {
        console.log('이미 셀프면접 생성 중...');
        return;
    }
    
    console.log('🔍 셀프면접 제한 체크 API 호출...');
    
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
            showSelfInterviewUpgradeModal(data);
        } else {
            console.log('✅ 제한 OK! 생성 진행');
            if (typeof proceedWithAICreation === 'function') {
                console.log('🤖 AI 면접 생성 진행');
                proceedWithAICreation();
            } else if (typeof proceedWithCreation === 'function') {
                console.log('📝 일반 셀프면접 생성 진행');
                proceedWithCreation();
            } else {
                console.error('❌ 생성 함수를 찾을 수 없습니다.');
            }
        }
    })
    .catch(error => {
        console.error('❌ 세션 제한 체크 실패:', error);
        alert('세션 생성 가능 여부를 확인할 수 없습니다.');
    });
}

function showSelfInterviewUpgradeModal(limitInfo) {
    console.log('🎨 셀프면접 모달 생성 시작...', limitInfo);
    
    const existingModal = document.getElementById('selfInterviewUpgradeModal');
    if (existingModal) {
        existingModal.remove();
    }
    
    const modal = document.createElement('div');
    modal.id = 'selfInterviewUpgradeModal';
    modal.className = 'modal-overlay';
    modal.innerHTML = `
        <div class="upgrade-modal">
            <div class="modal-header">
                <h2>셀프 면접 생성 한도 도달</h2>
                <button class="close-btn" onclick="closeSelfInterviewModal()">&times;</button>
            </div>
            <div class="modal-body">
                <div class="limit-icon">⚠️</div>
                <p class="limit-message">더 이상 셀프 면접을 생성할 수 없습니다.</p>
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
                    계속해서 면접 연습을 하려면 플랜을 업그레이드하세요.
                </p>
            </div>
            <div class="modal-footer">
                <button class="btn-cancel" onclick="closeSelfInterviewModal()">취소</button>
                <button class="btn-upgrade" onclick="goToUpgrade()">플랜 업그레이드</button>
            </div>
        </div>
    `;
    
    document.body.appendChild(modal);
    console.log('✅ 셀프면접 모달 DOM 추가 완료');
    
    setTimeout(() => {
        modal.classList.add('active');
        console.log('✅ 셀프면접 모달 active 클래스 추가');
    }, 10);
}

function closeSelfInterviewModal() {
    const modal = document.getElementById('selfInterviewUpgradeModal');
    if (modal) {
        modal.classList.remove('active');
        setTimeout(() => {
            modal.remove();
        }, 300);
    }
}

function getPlanDisplayName(planType) {
    const planNames = {
        'FREE': 'FREE',
        'BASIC': 'BASIC',
        'PRO': 'PRO',
        'TEAM': 'TEAM',
        'ENTERPRISE': 'ENTERPRISE',
        'NONE': 'NONE'
    };
    return planNames[planType] || planType;
}

function goToUpgrade() {
    window.location.href = '/payment/plans';
}