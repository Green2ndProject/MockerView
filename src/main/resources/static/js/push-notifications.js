const pushNotifications = {
    async requestPermission() {
        if (!('Notification' in window)) {
            console.warn('This browser does not support notifications');
            return false;
        }
        
        if (!('serviceWorker' in navigator)) {
            console.warn('Service Worker not supported');
            return false;
        }
        
        const permission = await Notification.requestPermission();
        
        if (permission === 'granted') {
            console.log('Notification permission granted');
            await this.subscribe();
            return true;
        } else {
            console.log('Notification permission denied');
            return false;
        }
    },
    
    async subscribe() {
        try {
            const registration = await navigator.serviceWorker.ready;
            
            const applicationServerKey = this.urlBase64ToUint8Array(
                'YOUR_VAPID_PUBLIC_KEY_HERE'
            );
            
            const subscription = await registration.pushManager.subscribe({
                userVisibleOnly: true,
                applicationServerKey: applicationServerKey
            });
            
            console.log('Push subscription:', subscription);
            
            await fetch('/api/notifications/subscribe', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': this.getCookie('Authorization')
                },
                body: JSON.stringify(subscription)
            });
            
            console.log('Subscription sent to server');
            
        } catch (error) {
            console.error('Failed to subscribe:', error);
        }
    },
    
    async unsubscribe() {
        try {
            const registration = await navigator.serviceWorker.ready;
            const subscription = await registration.pushManager.getSubscription();
            
            if (subscription) {
                await subscription.unsubscribe();
                console.log('Unsubscribed from push notifications');
                
                await fetch('/api/notifications/unsubscribe', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': this.getCookie('Authorization')
                    },
                    body: JSON.stringify(subscription)
                });
            }
        } catch (error) {
            console.error('Failed to unsubscribe:', error);
        }
    },
    
    async getSubscription() {
        try {
            const registration = await navigator.serviceWorker.ready;
            return await registration.pushManager.getSubscription();
        } catch (error) {
            console.error('Failed to get subscription:', error);
            return null;
        }
    },
    
    urlBase64ToUint8Array(base64String) {
        const padding = '='.repeat((4 - base64String.length % 4) % 4);
        const base64 = (base64String + padding)
            .replace(/\-/g, '+')
            .replace(/_/g, '/');
        
        const rawData = window.atob(base64);
        const outputArray = new Uint8Array(rawData.length);
        
        for (let i = 0; i < rawData.length; ++i) {
            outputArray[i] = rawData.charCodeAt(i);
        }
        
        return outputArray;
    },
    
    getCookie(name) {
        const value = `; ${document.cookie}`;
        const parts = value.split(`; ${name}=`);
        if (parts.length === 2) return parts.pop().split(';').shift();
        return '';
    }
};

window.pushNotifications = pushNotifications;
