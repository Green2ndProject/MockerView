package com.mockerview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgoraTokenDTO {
    private String token;
    private String channel;
    private String appId;
    private Integer uid;
    private Long expireTime;
}
