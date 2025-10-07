package com.mockerview.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Where(clause = "IS_DELETED = 0")
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

    @Column(name = "IS_DELETED", nullable = false, precision = 1)
    @ColumnDefault("0")
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "DELETED_AT")
    private LocalDateTime deletedAt;

    @Column(name = "WITHDRAWAL_REASON", length = 255)
    private String withdrawalReason;
}