const Auth = {
    getToken() {
        return localStorage.getItem(STORAGE_KEYS.TOKEN);
    },

    setToken(token) {
        localStorage.setItem(STORAGE_KEYS.TOKEN, token);
    },

    removeToken() {
        localStorage.removeItem(STORAGE_KEYS.TOKEN);
    },

    getUser() {
        const userData = localStorage.getItem(STORAGE_KEYS.USER);
        return userData ? JSON.parse(userData) : null;
    },

    setUser(user) {
        localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(user));
    },

    removeUser() {
        localStorage.removeItem(STORAGE_KEYS.USER);
    },

    isAuthenticated() {
        return !!this.getToken();
    },

    isAdmin() {
        const user = this.getUser();
        return user && (user.isAdmin === true || user.role === 'ADMIN');
    },

    requireAuth() {
        if (!this.isAuthenticated()) {
            showNotification(MESSAGES.UNAUTHORIZED, 'error');
            goToPage('login.html');
            return false;
        }
        return true;
    },

    logout() {
        this.removeToken();
        this.removeUser();
        showNotification(MESSAGES.LOGOUT_SUCCESS, 'success');
        goToPage('index.html');
    },

    async login(email, password) {
        try {
            const response = await fetch(buildURL(ENDPOINTS.AUTH.LOGIN), {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ email, password })
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || MESSAGES.SERVER_ERROR);
            }

            const data = await response.json();
            
            if (data.token) {
                this.setToken(data.token);
                this.setUser({
                    email: data.email,
                    nickname: data.nickname,
                    isAdmin: data.isAdmin || false
                });
                return data;
            } else {
                throw new Error('로그인 응답에 토큰이 없습니다.');
            }
        } catch (error) {
            throw error;
        }
    },

    async register(userData) {
        try {
            const response = await fetch(buildURL(ENDPOINTS.AUTH.REGISTER), {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(userData)
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || MESSAGES.SERVER_ERROR);
            }

            return await response.json();
        } catch (error) {
            throw error;
        }
    },

    getAuthHeaders() {
        const token = this.getToken();
        return token ? { 'Authorization': `Bearer ${token}` } : {};
    },

    handleUnauthorized() {
        this.removeToken();
        this.removeUser();
        showNotification(MESSAGES.UNAUTHORIZED, 'error');
        goToPage('login.html');
    }
};