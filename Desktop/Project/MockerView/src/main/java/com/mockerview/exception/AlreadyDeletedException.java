package com.mockerview.exception;

// 409 에러 사용자 정의 예외
public class AlreadyDeletedException extends RuntimeException {
    public AlreadyDeletedException(String message) {
        super(message);
    }

}
