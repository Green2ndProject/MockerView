const CACHE_NAME = 'mockerview-v1';
const OFFLINE_URL = '/offline.html';

self.addEventListener('install', event => {
    console.log('[SW] Installing Service Worker');
    event.waitUntil(
        caches.open(CACHE_NAME).then(cache => {
            return cache.addAll([
                '/',
                '/offline.html',
                '/css/common.css',
                '/images/192.png',
                '/images/512.png'
            ]);
        })
    );
    self.skipWaiting();
});

self.addEventListener('activate', event => {
    console.log('[SW] Activating Service Worker');
    event.waitUntil(
        caches.keys().then(cacheNames => {
            return Promise.all(
                cacheNames.map(cacheName => {
                    if (cacheName !== CACHE_NAME) {
                        console.log('[SW] Deleting old cache:', cacheName);
                        return caches.delete(cacheName);
                    }
                })
            );
        })
    );
    self.clients.claim();
});

self.addEventListener('fetch', event => {
    if (event.request.mode === 'navigate') {
        event.respondWith(
            fetch(event.request).catch(() => {
                return caches.match(OFFLINE_URL);
            })
        );
    }
});

self.addEventListener('push', event => {
    console.log('[SW] Push received:', event);
    
    if (!event.data) {
        console.log('[SW] No data in push event');
        return;
    }
    
    const data = event.data.json();
    console.log('[SW] Push data:', data);
    
    const options = {
        body: data.body,
        icon: data.icon || '/images/192.png',
        badge: '/images/192.png',
        data: {
            url: data.url || '/'
        },
        requireInteraction: true,
        vibrate: [200, 100, 200],
        tag: 'mockerview-notification'
    };
    
    event.waitUntil(
        self.registration.showNotification(data.title, options)
    );
});

self.addEventListener('notificationclick', event => {
    console.log('[SW] Notification clicked:', event);
    event.notification.close();
    
    const urlToOpen = event.notification.data?.url || '/';
    
    event.waitUntil(
        clients.matchAll({ type: 'window', includeUncontrolled: true })
            .then(windowClients => {
                for (let client of windowClients) {
                    if (client.url === urlToOpen && 'focus' in client) {
                        return client.focus();
                    }
                }
                if (clients.openWindow) {
                    return clients.openWindow(urlToOpen);
                }
            })
    );
});

self.addEventListener('notificationclose', event => {
    console.log('[SW] Notification closed:', event);
});
