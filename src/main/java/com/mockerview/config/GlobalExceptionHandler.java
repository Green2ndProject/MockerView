package com.mockerview.config;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.mockerview.exception.AlreadyDeletedException;

import java.util.Map;

@RestControllerAdvice // 모든 @Controller, @RestController에서 발생하는 예외를 잡는 클래스임을 선언
public class GlobalExceptionHandler {

    /**
     * Service 계층 등에서 발생하는 IllegalArgumentException을 처리합니다.
     * 주로 비밀번호 불일치, 유효하지 않은 요청 파라미터 등에 사용됩니다.
     * HTTP 400 Bad Request를 반환합니다.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 400
    public Map<String, String> handleIllegalArgumentException(IllegalArgumentException e) {
        // 클라이언트에게 { "message": "에러 메시지" } 형태로 JSON을 반환
        return Map.of("message", e.getMessage()); 
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED) // 401
    public Map<String, String> handleBadCredentialsException(BadCredentialsException e) {
        return Map.of("message", "인증에 실패했습니다. 세션이 유효하지 않습니다.");
    }

    @ExceptionHandler(DisabledException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN) // 403
    public Map<String, String> handleDisabledException(DisabledException e) {
        return Map.of("message", "탈퇴한 회원이거나 접근 권한이 없습니다.");
    }
    
    @ExceptionHandler(AlreadyDeletedException.class)
    @ResponseStatus(HttpStatus.CONFLICT) // 409
    public Map<String, String> handleAlreadyDeletedException(AlreadyDeletedException e) {
        return Map.of("message", e.getMessage());
    }
    // 필요하다면 다른 예외(NullPointerException, CustomException 등)도 추가할 수 있습니다.
}
