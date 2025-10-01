package com.mockerview.dto;

import lombok.Data;

@Data
public class SessionSearchDTO {
    private String keyword;
    private String status;
    private String sortBy;
    private String sortOrder;
}
