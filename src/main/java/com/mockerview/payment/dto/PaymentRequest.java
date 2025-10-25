package com.mockerview.payment.dto;

import com.mockerview.payment.PaymentMethod;
import com.mockerview.entity.Subscription;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentRequest {
    private Long userId;
    private Subscription.PlanType planType;
    private PaymentMethod paymentMethod;
    private Integer amount;
    private String orderId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
}
