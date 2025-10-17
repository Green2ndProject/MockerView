package com.mockerview.dto;

import lombok.Data;

@Data
public class FindUsernameRequest {
    private String name;
    private String email;
}
