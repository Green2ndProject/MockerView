package com.mockerview.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanType planType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;
    
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime nextBillingDate;
    
    @Builder.Default
    private Boolean autoRenew = true;
    
    @Builder.Default
    private Integer usedSessions = 0;
    
    private Integer sessionLimit;
    
    @Builder.Default
    private Integer usedReviewReads = 0;
    
    private Integer reviewReadLimit;
    
    private String paymentMethodId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public enum PlanType {
        FREE, BASIC, PRO, TEAM, ENTERPRISE
    }
    
    public enum SubscriptionStatus {
        ACTIVE, CANCELLED, EXPIRED, PAYMENT_FAILED, TRIAL
    }
}
