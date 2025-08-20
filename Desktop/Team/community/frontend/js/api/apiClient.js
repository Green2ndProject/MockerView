class APIClient {
    static async request(url, options = {}) {
        const config = {
            headers: {
                'Content-Type': 'application/json',
                ...Auth.getAuthHeaders(),
                ...options.headers
            },
            ...options
        };

        try {
            const response = await fetch(url, config);
            
            if (response.status === 401) {
                Auth.handleUnauthorized();
                throw new Error(MESSAGES.UNAUTHORIZED);
            }

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || MESSAGES.SERVER_ERROR);
            }

            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                return await response.json();
            }
            
            return await response.text();
        } catch (error) {
            if (error.name === 'TypeError') {
                throw new Error(MESSAGES.NETWORK_ERROR);
            }
            throw error;
        }
    }

    static async get(url, params = {}) {
        const queryString = new URLSearchParams(params).toString();
        const fullUrl = queryString ? `${url}?${queryString}` : url;
        return this.request(fullUrl);
    }

    static async post(url, data = {}) {
        return this.request(url, {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }

    static async put(url, data = {}) {
        return this.request(url, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    }

    static async delete(url) {
        return this.request(url, {
            method: 'DELETE'
        });
    }

    static async uploadFile(url, formData) {
        const config = {
            method: 'POST',
            headers: {
                ...Auth.getAuthHeaders()
            },
            body: formData
        };

        try {
            const response = await fetch(url, config);
            
            if (response.status === 401) {
                Auth.handleUnauthorized();
                throw new Error(MESSAGES.UNAUTHORIZED);
            }

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || MESSAGES.SERVER_ERROR);
            }

            return await response.json();
        } catch (error) {
            if (error.name === 'TypeError') {
                throw new Error(MESSAGES.NETWORK_ERROR);
            }
            throw error;
        }
    }
}