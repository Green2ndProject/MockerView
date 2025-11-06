function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}

async function sendBroadcast(title, body, url = '/') {
    const token = getCookie('Authorization');
    
    if (!token) {
        console.error('❌ 로그인이 필요합니다');
        alert('로그인이 필요합니다');
        return;
    }
    
    try {
        const response = await fetch('https://mockerview.net/api/push/broadcast', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            credentials: 'include',
            body: JSON.stringify({ title, body, url })
        });
        
        const result = await response.json();
        
        if (response.ok) {
            console.log('✅', result.message);
            alert(`✅ ${result.message}`);
        } else {
            console.error('❌', result.message);
            alert(`❌ ${result.message}`);
        }
        
        return result;
    } catch (error) {
        console.error('❌ 전송 실패:', error);
        alert('❌ 전송 실패: ' + error.message);
    }
}

console.log('📢 Admin Broadcast Helper loaded!');
console.log('사용법: sendBroadcast("제목", "내용", "URL")');
