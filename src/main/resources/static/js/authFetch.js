function getAuthToken() {
    const cookieToken = document.cookie
        .split('; ')
        .find(row => row.startsWith('Authorization='))
        ?.split('=')[1];
    
    if (cookieToken) return cookieToken;
    
    return document.cookie
        .split('; ')
        .find(row => row.startsWith('access_token='))
        ?.split('=')[1] || localStorage.getItem('access_token');
}

function authFetch(url, options = {}) {
    const token = getAuthToken();
    
    const headers = {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        ...options.headers
    };
    
    if (token) {
        headers['Authorization'] = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
    }
    
    return fetch(url, {
        ...options,
        credentials: 'include',
        headers
    }).then(response => {
        if (response.status === 401 || response.status === 403) {
            localStorage.removeItem('access_token');
            window.location.href = '/auth/login';
            throw new Error('Unauthorized');
        }
        return response;
    });
}

function authGet(url, options = {}) {
    return authFetch(url, {
        method: 'GET',
        ...options
    });
}

function authPost(url, data, options = {}) {
    return authFetch(url, {
        method: 'POST',
        body: typeof data === 'string' ? data : JSON.stringify(data),
        ...options
    });
}

function authPut(url, data, options = {}) {
    return authFetch(url, {
        method: 'PUT',
        body: typeof data === 'string' ? data : JSON.stringify(data),
        ...options
    });
}

function authDelete(url, options = {}) {
    return authFetch(url, {
        method: 'DELETE',
        ...options
    });
}

function authFormData(url, formData, options = {}) {
    const token = getAuthToken();
    
    const headers = {
        'Accept': 'application/json',
        ...options.headers
    };
    
    if (token) {
        headers['Authorization'] = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
    }
    
    return fetch(url, {
        method: 'POST',
        body: formData,
        credentials: 'include',
        headers,
        ...options
    }).then(response => {
        if (response.status === 401 || response.status === 403) {
            localStorage.removeItem('access_token');
            window.location.href = '/auth/login';
            throw new Error('Unauthorized');
        }
        return response;
    });
}
