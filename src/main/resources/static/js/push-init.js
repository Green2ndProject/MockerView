if ('serviceWorker' in navigator && 'PushManager' in window) {
    navigator.serviceWorker.register('/service-worker.js')
        .then(registration => {
            console.log('✅ Service Worker registered:', registration.scope);
            
            setTimeout(async () => {
                if (window.pushNotifications && Notification.permission === 'default') {
                    console.log('🔔 Requesting notification permission...');
                    await window.pushNotifications.requestPermission();
                } else if (Notification.permission === 'granted') {
                    const sub = await registration.pushManager.getSubscription();
                    if (!sub && window.pushNotifications) {
                        console.log('🔔 Permission granted but not subscribed, subscribing now...');
                        await window.pushNotifications.subscribe();
                    }
                }
            }, 3000);
        })
        .catch(error => {
            console.error('❌ Service Worker registration failed:', error);
        });
} else {
    console.warn('⚠️ Service Worker or Push API not supported');
}