function logout() {
    fetch('/api/auth/logout', {
        method: 'POST',
        credentials: 'include'
    })
    .then(response => response.json())
    .then(data => {
        console.log('✅ 로그아웃 성공:', data.message);
        window.location.href = '/auth/login';
    })
    .catch(error => {
        console.error('❌ 로그아웃 실패:', error);
        alert('로그아웃 중 오류가 발생했습니다.');
    });
}
