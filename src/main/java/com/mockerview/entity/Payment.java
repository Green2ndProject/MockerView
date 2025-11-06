package com.mockerview.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;
    
    private String orderId;
    private String paymentKey;
    private String transactionId;
    
    @Enumerated(EnumType.STRING)
    private PaymentMethod method;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    
    private BigDecimal amount;
    
    @Builder.Default
    private String currency = "KRW";
    
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime failedAt;
    private String failureReason;
    
    @Column(columnDefinition = "TEXT")
    private String receiptUrl;
    
    public enum PaymentMethod {
        CARD, KAKAOPAY, TOSSPAY, TRANSFER
    }
    
    public enum PaymentStatus {
        PENDING, COMPLETED, FAILED, CANCELLED, REFUNDED
    }
}
