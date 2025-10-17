function redirectToPayment(planType) {
    const isLoggedIn = document.cookie.includes('Authorization');
    
    if (!isLoggedIn) {
        sessionStorage.setItem('redirectAfterLogin', `/payment/checkout-page?planType=${planType}`);
        document.cookie = `redirectAfterLogin=/payment/checkout-page?planType=${planType}; path=/; max-age=600`;
        alert('로그인이 필요합니다');
        window.location.href = '/auth/login';
    } else {
        window.location.href = `/payment/checkout-page?planType=${planType}`;
    }
}

function selectPlan(planType) {
    redirectToPayment(planType);
}
