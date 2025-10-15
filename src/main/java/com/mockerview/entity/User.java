package com.mockerview.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(unique = true)
    private String username;

    private String password;

    private String email;

    public enum UserRole {
        STUDENT, HOST, REVIEWER
    }

    @Column(name = "IS_DELETED", nullable = false)
    @ColumnDefault("0")
    @Builder.Default
    private Short isDeleted = 0;

    @Column(name = "DELETED_AT")
    private LocalDateTime deletedAt;

    @Column(name = "WITHDRAWAL_REASON", length = 255)
    private String withdrawalReason;
    
    public boolean isDeleted() {
        return isDeleted != null && isDeleted == 1;
    }
    
    public void setDeleted(boolean deleted) {
        this.isDeleted = deleted ? (short) 1 : (short) 0;
    }
}