const pushNotifications = {
    registration: null,
    subscription: null,
    vapidPublicKey: null,

    async init() {
        if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
            console.warn('❌ Push notifications not supported');
            return false;
        }

        try {
            this.registration = await navigator.serviceWorker.ready;
            await this.loadVapidKey();
            console.log('✅ Push notifications initialized');
            return true;
        } catch (error) {
            console.error('❌ Push init failed:', error);
            return false;
        }
    },

    async loadVapidKey() {
        try {
            const response = await fetch('/api/notifications/vapid-public-key');
            const data = await response.json();
            this.vapidPublicKey = data.publicKey;
            console.log('✅ VAPID public key loaded');
        } catch (error) {
            console.error('❌ Failed to load VAPID key:', error);
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

    async requestPermission() {
        const permission = await Notification.requestPermission();
        
        if (permission === 'granted') {
            console.log('✅ Notification permission granted');
            await this.subscribe();
            return true;
        } else if (permission === 'denied') {
            console.warn('❌ Notification permission denied');
            return false;
        } else {
            console.log('⚠️ Notification permission dismissed');
            return false;
        }
    },

    async subscribe() {
        try {
            if (!this.vapidPublicKey) {
                await this.loadVapidKey();
            }

            const subscription = await this.registration.pushManager.subscribe({
                userVisibleOnly: true,
                applicationServerKey: this.urlBase64ToUint8Array(this.vapidPublicKey)
            });

            this.subscription = subscription;
            console.log('✅ Push subscription created:', subscription);

            await this.sendSubscriptionToServer(subscription);
            return subscription;
        } catch (error) {
            console.error('❌ Push subscription failed:', error);
            throw error;
        }
    },

    async sendSubscriptionToServer(subscription) {
        const subscriptionJson = subscription.toJSON();
        
        const payload = {
            endpoint: subscriptionJson.endpoint,
            keys: {
                p256dh: subscriptionJson.keys.p256dh,
                auth: subscriptionJson.keys.auth
            }
        };

        try {
            const response = await fetch('/api/notifications/subscribe', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(payload)
            });

            if (response.ok) {
                console.log('✅ Subscription sent to server');
            } else {
                console.error('❌ Failed to send subscription to server');
            }
        } catch (error) {
            console.error('❌ Error sending subscription:', error);
        }
    },

    async unsubscribe() {
        try {
            const subscription = await this.registration.pushManager.getSubscription();
            
            if (subscription) {
                await subscription.unsubscribe();
                
                await fetch('/api/notifications/unsubscribe', {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        endpoint: subscription.endpoint
                    })
                });

                console.log('✅ Push unsubscribed');
                this.subscription = null;
            }
        } catch (error) {
            console.error('❌ Unsubscribe failed:', error);
        }
    },

    async getSubscription() {
        try {
            const subscription = await this.registration.pushManager.getSubscription();
            this.subscription = subscription;
            return subscription;
        } catch (error) {
            console.error('❌ Failed to get subscription:', error);
            return null;
        }
    },

    isSubscribed() {
        return this.subscription !== null;
    }
};

if (typeof window !== 'undefined') {
    window.pushNotifications = pushNotifications;
    
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            pushNotifications.init();
        });
    } else {
        pushNotifications.init();
    }
}
