package com.mockerview.dto;

import java.time.LocalDateTime;

import com.mockerview.entity.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchResponse {
    Long id;
    LocalDateTime lastLoginDate;
    String email;
    String name;
    String role;
    String username;

    public static UserSearchResponse from(User user){
        return UserSearchResponse.builder()
                .id(user.getId())
                .lastLoginDate(user.getLastLoginDate())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().toString())
                .username(user.getUsername())
                .build();
    }
}
