package com.mockerview.payment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResult {
    private boolean success;
    private String paymentKey;
    private String orderId;
    private String message;
    private String receiptUrl;
    private String virtualAccountNumber;
    private String virtualAccountBank;
}
