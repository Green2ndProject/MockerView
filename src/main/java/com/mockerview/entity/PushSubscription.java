package com.mockerview.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "push_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushSubscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, unique = true, length = 500)
    private String endpoint;
    
    @Column(nullable = false, length = 500)
    private String p256dh;
    
    @Column(nullable = false, length = 500)
    private String auth;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "last_used")
    private LocalDateTime lastUsed;
    
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
}
