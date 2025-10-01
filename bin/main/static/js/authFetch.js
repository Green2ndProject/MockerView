/**
 * 
 * @param {string} url - 요청을 보낼 URL
 * @param {Object} [options={}] - fetch()의 options 객체 (method, headers, body 등)
 * @returns {Promise<Response>} fetch()의 Promise
 */
export function authFetch(url, options = {}) {
    // 1. localStorage에서 저장된 JWT 토큰을 가져옵니다.
    const token = localStorage.getItem('jwt_token'); 

    // 2. 헤더 객체가 없으면 새로 생성합니다.
    options.headers = options.headers || {}; 

    // 3. 토큰이 존재하면 Authorization 헤더에 추가합니다.
    if (token) {
        // 서버에서 'Bearer eyJ...' 형식으로 보냈으므로, 그대로 사용합니다.
        options.headers['Authorization'] = `Bearer ${token}`; 
    }

    // 4. Content-Type이 설정되지 않았다면 JSON 기본값으로 설정합니다.
    //    (로그인 요청처럼 JSON 본문이 있는 경우에 유용합니다. POST, PUT 요청 시 필요)
    if (!options.headers['Content-Type'] && options.method && options.method !== 'GET' && options.body) {
         options.headers['Content-Type'] = 'application/json';
    }
    
    // 5. 기본 fetch 함수를 호출하고 결과를 반환합니다.
    return fetch(url, options);
}