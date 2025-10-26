(function() {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
        console.warn('⚠️ Push notifications not supported in this browser');
        return;
    }

    navigator.serviceWorker.register('/service-worker.js', { scope: '/' })
        .then(registration => {
            console.log('✅ Service Worker registered:', registration.scope);
            
            if (window.pushNotifications) {
                window.pushNotifications.init();
            }
            return registration.pushManager.getSubscription();
        })
        .then(subscription => {
            if (!subscription && Notification.permission === 'default') {
                setTimeout(() => {
                    if (window.showPushPrompt && typeof window.showPushPrompt === 'function') {
                        window.showPushPrompt();
                    }
                }, 3000);
            } else if (subscription) {
                console.log('✅ Already subscribed to push notifications');
            }
        })
        .catch(error => {
            console.error('❌ Service Worker error:', error);
        });
})();
