document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('createForm');
    
    if (!form) {
        console.warn('createForm not found');
        return;
    }

    form.addEventListener('submit', function(e) {
        e.preventDefault();
        
        console.log('🔍 셀프면접 생성 버튼 클릭 - 제한 체크 시작');
        
        if (typeof checkSessionLimitForSelfInterview === 'function') {
            console.log('✅ checkSessionLimitForSelfInterview 함수 발견 - 제한 체크 실행');
            checkSessionLimitForSelfInterview();
        } else {
            console.log('⚠️ checkSessionLimitForSelfInterview 함수 없음 - 바로 생성 시도');
            proceedWithCreation();
        }
    });
    
    window.proceedWithCreation = function() {
        const submitBtn = form.querySelector('button[type="submit"]');
        const originalText = submitBtn.textContent;
        
        submitBtn.disabled = true;
        submitBtn.textContent = '생성 중...';

        const formData = {
            title: document.getElementById('title').value,
            sessionType: document.querySelector('input[name="sessionType"]:checked').value,
            difficulty: document.getElementById('difficulty').value,
            category: document.getElementById('category').value,
            questionCount: parseInt(document.getElementById('questionCount').value)
        };

        if (!formData.title.trim()) {
            alert('면접 제목을 입력해주세요.');
            submitBtn.disabled = false;
            submitBtn.textContent = originalText;
            return;
        }

        fetch('/api/selfinterview', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            credentials: 'include',
            body: JSON.stringify(formData)
        })
        .then(function(res) {
            if (!res.ok) {
                return res.json().then(function(err) {
                    throw new Error(err.message || '생성 실패');
                });
            }
            return res.json();
        })
        .then(function(data) {
            if (data.success) {
                alert('셀프면접이 생성되었습니다!');
                window.location.href = data.redirectUrl || '/selfinterview/room/' + data.sessionId;
            } else {
                if (data.limitExceeded || data.error === 'SESSION_LIMIT_EXCEEDED') {
                    if (typeof showSelfInterviewUpgradeModal === 'function' && data.limitInfo) {
                        showSelfInterviewUpgradeModal(data.limitInfo);
                    } else {
                        showUpgradeModal();
                    }
                } else {
                    throw new Error(data.message || '알 수 없는 오류');
                }
            }
        })
        .catch(function(err) {
            console.error('Error:', err);
            
            if (err.message && err.message.includes('한도')) {
                if (typeof showSelfInterviewUpgradeModal === 'function') {
                    fetch('/api/session-limit/check')
                        .then(res => res.json())
                        .then(data => showSelfInterviewUpgradeModal(data))
                        .catch(() => showUpgradeModal());
                } else {
                    showUpgradeModal();
                }
            } else {
                alert('셀프면접 생성 실패: ' + err.message);
            }
            
            submitBtn.disabled = false;
            submitBtn.textContent = originalText;
        });
    };
    
    function showUpgradeModal() {
        fetch('/api/subscription/check')
            .then(res => res.json())
            .then(data => {
                const modalHTML = `
                    <div class="modal" id="upgradeModal" style="display: flex;">
                        <div class="upgrade-modal">
                            <div class="modal-header" style="padding: 32px 32px 24px 32px; border-bottom: 1px solid #e9ecef;">
                                <div style="display: flex; align-items: center; gap: 12px;">
                                    <div style="width: 48px; height: 48px; background: #fff3cd; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 24px;">⚠️</div>
                                    <h2 style="margin: 0; font-size: 24px; font-weight: 700; color: #212529;">세션 생성 한도 도달</h2>
                                </div>
                                <button class="close-modal" onclick="closeUpgradeModal()" style="position: absolute; top: 24px; right: 24px; background: none; border: none; font-size: 28px; cursor: pointer; color: #adb5bd; transition: color 0.2s;">&times;</button>
                            </div>
                            <div class="modal-body" style="padding: 32px;">
                                <div class="limit-info-box" style="background: #f8f9fa; border-radius: 12px; padding: 24px; margin-bottom: 28px; border: 1px solid #e9ecef;">
                                    <div class="limit-info-item" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 16px; border-bottom: 1px solid #dee2e6;">
                                        <span class="limit-label" style="color: #6c757d; font-size: 15px; font-weight: 500;">현재 플랜</span>
                                        <span class="limit-value plan-badge" style="background: #495057; color: white; padding: 6px 16px; border-radius: 20px; font-weight: 600; font-size: 14px; letter-spacing: 0.5px;">${data.currentPlan}</span>
                                    </div>
                                    <div class="limit-info-item" style="display: flex; justify-content: space-between; align-items: center;">
                                        <span class="limit-label" style="color: #6c757d; font-size: 15px; font-weight: 500;">사용한 세션</span>
                                        <span class="limit-value" style="color: #dc3545; font-weight: 700; font-size: 20px;">${data.usedSessions} <span style="color: #adb5bd; font-weight: 500;">/ ${data.sessionLimit}</span></span>
                                    </div>
                                </div>
                                
                                <div class="upgrade-message" style="margin-bottom: 28px;">
                                    <p style="font-size: 15px; color: #6c757d; margin-bottom: 20px; line-height: 1.6; text-align: center;">더 많은 세션을 생성하려면 플랜을 업그레이드하세요</p>
                                    <div style="background: #f8f9fa; border-radius: 12px; padding: 20px; border: 1px solid #e9ecef;">
                                        <ul class="upgrade-benefits" style="list-style: none; padding: 0; margin: 0;">
                                            <li style="padding: 10px 0; color: #212529; font-size: 14px; display: flex; align-items: center; gap: 12px;">
                                                <span style="width: 24px; height: 24px; background: #d1ecf1; border-radius: 6px; display: flex; align-items: center; justify-content: center; font-size: 12px;">✓</span>
                                                <span style="font-weight: 500;">무제한 세션 생성</span>
                                            </li>
                                            <li style="padding: 10px 0; color: #212529; font-size: 14px; display: flex; align-items: center; gap: 12px;">
                                                <span style="width: 24px; height: 24px; background: #d1ecf1; border-radius: 6px; display: flex; align-items: center; justify-content: center; font-size: 12px;">✓</span>
                                                <span style="font-weight: 500;">고급 AI 피드백 기능</span>
                                            </li>
                                            <li style="padding: 10px 0; color: #212529; font-size: 14px; display: flex; align-items: center; gap: 12px;">
                                                <span style="width: 24px; height: 24px; background: #d1ecf1; border-radius: 6px; display: flex; align-items: center; justify-content: center; font-size: 12px;">✓</span>
                                                <span style="font-weight: 500;">상세한 분석 리포트</span>
                                            </li>
                                            <li style="padding: 10px 0; color: #212529; font-size: 14px; display: flex; align-items: center; gap: 12px;">
                                                <span style="width: 24px; height: 24px; background: #d1ecf1; border-radius: 6px; display: flex; align-items: center; justify-content: center; font-size: 12px;">✓</span>
                                                <span style="font-weight: 500;">우선 고객 지원</span>
                                            </li>
                                        </ul>
                                    </div>
                                </div>
                                
                                <div class="form-actions" style="display: flex; gap: 12px;">
                                    <button type="button" onclick="closeUpgradeModal()" onmouseover="this.style.background='#e9ecef'" onmouseout="this.style.background='#f8f9fa'" style="flex: 1; padding: 14px; background: #f8f9fa; color: #6c757d; border: 1px solid #dee2e6; border-radius: 8px; font-size: 15px; font-weight: 600; cursor: pointer; transition: all 0.2s;">취소</button>
                                    <button type="button" onclick="window.location.href='/payment/plans'" onmouseover="this.style.background='#5568d3'; this.style.transform='translateY(-2px)'; this.style.boxShadow='0 4px 12px rgba(102, 126, 234, 0.4)'" onmouseout="this.style.background='#667eea'; this.style.transform='translateY(0)'; this.style.boxShadow='0 2px 8px rgba(102, 126, 234, 0.25)'" style="flex: 2; padding: 14px; background: #667eea; color: white; border: none; border-radius: 8px; font-size: 15px; font-weight: 600; cursor: pointer; transition: all 0.2s; box-shadow: 0 2px 8px rgba(102, 126, 234, 0.25);">플랜 업그레이드 →</button>
                                </div>
                            </div>
                        </div>
                    </div>
                `;
                
                document.body.insertAdjacentHTML('beforeend', modalHTML);
            })
            .catch(err => {
                alert('세션 한도를 확인할 수 없습니다.');
            });
    }
    
    window.closeUpgradeModal = function() {
        const modal = document.getElementById('upgradeModal');
        if (modal) {
            modal.remove();
        }
        
        const submitBtn = form.querySelector('button[type="submit"]');
        submitBtn.disabled = false;
        submitBtn.textContent = '면접 시작';
    };
});