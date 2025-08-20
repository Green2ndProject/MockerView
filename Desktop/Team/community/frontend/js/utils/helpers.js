function showElement(element) {
    if (element) {
        element.classList.remove('hidden');
    }
}

function hideElement(element) {
    if (element) {
        element.classList.add('hidden');
    }
}

function setLoading(button, loading = true) {
    if (!button) return;
    
    if (loading) {
        button.disabled = true;
        button.dataset.originalText = button.textContent;
        button.textContent = '처리 중...';
    } else {
        button.disabled = false;
        button.textContent = button.dataset.originalText || button.textContent;
    }
}

function showNotification(message, type = 'success') {
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.textContent = message;
    document.body.appendChild(notification);

    setTimeout(() => {
        notification.remove();
    }, UI.NOTIFICATION_DURATION);
}

function formatDate(dateString) {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffMins < 1) return '방금 전';
    if (diffMins < 60) return `${diffMins}분 전`;
    if (diffHours < 24) return `${diffHours}시간 전`;
    if (diffDays < 7) return `${diffDays}일 전`;
    
    return date.toLocaleDateString('ko-KR');
}

function sanitizeHTML(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

function truncateText(text, length = 100) {
    if (!text || text.length <= length) return text;
    return text.substring(0, length) + '...';
}

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

function getURLParams() {
    const params = new URLSearchParams(window.location.search);
    const result = {};
    for (const [key, value] of params) {
        result[key] = value;
    }
    return result;
}

function validateEmail(email) {
    return VALIDATION.EMAIL_REGEX.test(email);
}

function validatePassword(password) {
    return password && password.length >= VALIDATION.MIN_PASSWORD_LENGTH;
}

function validateFileSize(file) {
    return file.size <= VALIDATION.MAX_FILE_SIZE;
}

function validateFileType(file) {
    return VALIDATION.ALLOWED_FILE_TYPES.includes(file.type);
}

function clearFormErrors(form) {
    const errorElements = form.querySelectorAll('.error-message');
    errorElements.forEach(element => {
        hideElement(element);
        element.textContent = '';
    });
    
    const inputElements = form.querySelectorAll('.form-control');
    inputElements.forEach(element => element.classList.remove('error'));
}

function showFormError(fieldName, message) {
    const field = document.getElementById(fieldName);
    const errorElement = document.getElementById(`${fieldName}-error`);
    
    if (field) field.classList.add('error');
    if (errorElement) {
        errorElement.textContent = message;
        showElement(errorElement);
    }
}

function buildURL(endpoint, params = {}) {
    let url = API_BASE_URL + endpoint;
    
    Object.keys(params).forEach(key => {
        url = url.replace(`{${key}}`, params[key]);
    });
    
    return url;
}

function goToPage(url) {
    window.location.href = url;
}

function goBack() {
    if (window.history.length > 1) {
        window.history.back();
    } else {
        goToPage('index.html');
    }
}