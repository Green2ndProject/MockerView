package com.mockerview.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivateMessage {

    Long senderId;
    Long receiverId;
    String content;
    LocalDateTime timestamp;
}
