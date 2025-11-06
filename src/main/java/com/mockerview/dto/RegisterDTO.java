package com.mockerview.dto;

import lombok.Data;

@Data
public class RegisterDTO {
    private String username;
    private String password;
    private String name;
    private String email;
    private String role;
    private String emailVerificationCode;
    private String recaptchaToken;
}