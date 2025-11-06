package com.mockerview.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.mockerview.exception.AlreadyDeletedException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgumentException(IllegalArgumentException e) {
        return Map.of("message", e.getMessage()); 
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> handleBadCredentialsException(BadCredentialsException e) {
        return Map.of("message", "인증에 실패했습니다. 세션이 유효하지 않습니다.");
    }

    @ExceptionHandler(DisabledException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleDisabledException(DisabledException e) {
        return Map.of("message", "탈퇴한 회원이거나 접근 권한이 없습니다.");
    }
    
    @ExceptionHandler(AlreadyDeletedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleAlreadyDeletedException(AlreadyDeletedException e) {
        return Map.of("message", e.getMessage());
    }

    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbortException(ClientAbortException e) {
        log.debug("Client closed connection: {}", e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException e) {
        log.debug("Resource not found: {}", e.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Resource not found");
        response.put("path", e.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.APPLICATION_JSON)
            .body(response);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e, HttpServletRequest request) {
        String contentType = request.getContentType();
        String acceptHeader = request.getHeader("Accept");
        
        if ((contentType != null && contentType.startsWith("image/")) ||
            (acceptHeader != null && acceptHeader.contains("image/"))) {
            log.debug("Image request error ignored: {}", e.getMessage());
            return null;
        }
        
        log.error("=== 전체 예외 발생 ===");
        log.error("예외 타입: {}", e.getClass().getName());
        log.error("예외 메시지: {}", e.getMessage());
        log.error("스택 트레이스:", e);
        
        Throwable cause = e.getCause();
        int depth = 1;
        while (cause != null && depth <= 5) {
            log.error("원인 #{}: {} - {}", depth, cause.getClass().getName(), cause.getMessage());
            cause = cause.getCause();
            depth++;
        }
        
        Map<String, String> error = new HashMap<>();
        error.put("error", e.getMessage() != null ? e.getMessage() : "알 수 없는 오류");
        error.put("type", e.getClass().getSimpleName());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(error);
    }
}
