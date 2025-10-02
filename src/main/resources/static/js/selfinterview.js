document.getElementById('createForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    
    const title = document.getElementById('title').value;
    const questionCount = parseInt(document.getElementById('questionCount').value);
    
    if (!title.trim()) {
        alert('면접 제목을 입력해주세요.');
        return;
    }
    
    const submitBtn = this.querySelector('button[type="submit"]');
    submitBtn.disabled = true;
    submitBtn.textContent = '생성 중...';
    
    try {
        const response = await authFetch('/api/selfinterview', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                title: title,
                questionCount: questionCount
            })
        });
        
        const data = await response.json();
        
        if (data.success) {
            alert(data.message);
            window.location.href = `/selfinterview/room/${data.sessionId}`;
        } else {
            alert('생성 실패: ' + data.message);
            submitBtn.disabled = false;
            submitBtn.textContent = '면접 시작';
        }
    } catch (error) {
        console.error('Error:', error);
        alert('면접 생성 중 오류가 발생했습니다.');
        submitBtn.disabled = false;
        submitBtn.textContent = '면접 시작';
    }
});
