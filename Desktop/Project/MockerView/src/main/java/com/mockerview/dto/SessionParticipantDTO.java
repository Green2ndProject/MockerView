package com.mockerview.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.mockerview.entity.SessionParticipant;
import com.mockerview.entity.User;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionParticipantDTO {
    private Long id;
    private Long sessionId;
    private Long userId;
    private String userName;
    private User.UserRole role;
    private Boolean isOnline;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime joinedAt;
    
    public static SessionParticipantDTO from(SessionParticipant entity) {
        return SessionParticipantDTO.builder()
                .id(entity.getId())
                .sessionId(entity.getSession().getId())
                .userId(entity.getUser().getId())
                .userName(entity.getUser().getName())
                .role(entity.getRole())
                .isOnline(entity.getIsOnline())
                .joinedAt(entity.getJoinedAt())
                .build();
    }
}