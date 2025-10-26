let tokenRefreshInterval;

function startTokenRefresh() {
    tokenRefreshInterval = setInterval(async () => {
        await refreshAccessToken();
    }, 50 * 60 * 1000);
    
    console.log('✅ Token auto-refresh started (every 50 minutes)');
}

async function refreshAccessToken() {
    try {
        const response = await fetch('/api/auth/refresh', {
            method: 'POST',
            credentials: 'include'
        });
        
        if (response.ok) {
            console.log('✅ Access token refreshed');
            updateLastActivity();
        } else {
            console.warn('⚠️ Token refresh failed, redirecting to login');
            clearInterval(tokenRefreshInterval);
            window.location.href = '/user/login';
        }
    } catch (error) {
        console.error('❌ Token refresh error:', error);
    }
}

function updateLastActivity() {
    localStorage.setItem('lastActivity', new Date().toISOString());
}

function getLastActivity() {
    return localStorage.getItem('lastActivity');
}

function checkInactivity() {
    const lastActivity = getLastActivity();
    if (!lastActivity) return;
    
    const lastDate = new Date(lastActivity);
    const now = new Date();
    const daysSinceLastActivity = Math.floor((now - lastDate) / (1000 * 60 * 60 * 24));
    
    if (daysSinceLastActivity >= 3 && window.pushNotifications) {
        console.log(`⚠️ User inactive for ${daysSinceLastActivity} days`);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    if (isLoggedIn()) {
        startTokenRefresh();
        updateLastActivity();
        checkInactivity();
        
        document.addEventListener('click', updateLastActivity);
        document.addEventListener('keypress', updateLastActivity);
        document.addEventListener('scroll', updateLastActivity);
    }
});

function isLoggedIn() {
    return document.cookie.split(';').some(c => c.trim().startsWith('Authorization='));
}

window.addEventListener('beforeunload', () => {
    updateLastActivity();
});
