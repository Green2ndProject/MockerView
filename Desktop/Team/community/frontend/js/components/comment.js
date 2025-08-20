const API_BASE_URL = 'http://localhost:8080/api';

const ENDPOINTS = {
    AUTH: {
        LOGIN: '/auth/login',
        REGISTER: '/auth/register',
        ME: '/auth/me'
    },
    POSTS: {
        LIST: '/posts',
        CREATE: '/posts',
        DETAIL: '/posts',
        UPDATE: '/posts',
        DELETE: '/posts',
        LIKE: '/posts/{id}/like'
    },
    COMMENTS: {
        LIST: '/posts/{postId}/comments',
        CREATE: '/posts/{postId}/comments',
        UPDATE: '/comments/{id}',
        DELETE: '/comments/{id}'
    },
    BOOKMARKS: {
        TOGGLE: '/bookmarks/posts/{postId}',
        MY_LIST: '/bookmarks/my'
    },
    MESSAGES: {
        SEND: '/messages',
        RECEIVED: '/messages/received',
        SENT: '/messages/sent',
        READ: '/messages/{id}/read',
        DELETE: '/messages/{id}'
    },
    USERS: {
        PROFILE: '/users/profile',
        POSTS: '/user/posts',
        COMMENTS: '/user/comments',
        STATS: '/user/stats'
    },
    FILES: {
        UPLOAD: '/files/upload'
    }
};

const STORAGE_KEYS = {
    TOKEN: 'auth_token',
    USER: 'user_data'
};

const MESSAGES = {
    NETWORK_ERROR: '네트워크 오류가 발생했습니다.',
    UNAUTHORIZED: '로그인이 필요합니다.',
    SERVER_ERROR: '서버 오류가 발생했습니다.',
    SUCCESS: '성공적으로 처리되었습니다.',
    REGISTER_SUCCESS: '회원가입이 완료되었습니다.',
    LOGIN_SUCCESS: '로그인되었습니다.',
    LOGOUT_SUCCESS: '로그아웃되었습니다.',
    POST_CREATED: '게시글이 작성되었습니다.',
    POST_UPDATED: '게시글이 수정되었습니다.',
    POST_DELETED: '게시글이 삭제되었습니다.',
    COMMENT_CREATED: '댓글이 작성되었습니다.',
    COMMENT_DELETED: '댓글이 삭제되었습니다.',
    BOOKMARK_ADDED: '북마크에 추가되었습니다.',
    BOOKMARK_REMOVED: '북마크에서 제거되었습니다.',
    MESSAGE_SENT: '쪽지가 전송되었습니다.',
    MESSAGE_DELETED: '쪽지가 삭제되었습니다.'
};

const VALIDATION = {
    EMAIL_REGEX: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
    MIN_PASSWORD_LENGTH: 8,
    MIN_CONTENT_LENGTH: 10,
    MAX_TITLE_LENGTH: 255,
    MAX_FILE_SIZE: 5 * 1024 * 1024,
    ALLOWED_FILE_TYPES: ['image/jpeg', 'image/png', 'image/gif']
};

const PAGINATION = {
    DEFAULT_SIZE: 10,
    MAX_SIZE: 50
};

const UI = {
    DEBOUNCE_DELAY: 300,
    LOADING_DELAY: 200,
    NOTIFICATION_DURATION: 3000
};