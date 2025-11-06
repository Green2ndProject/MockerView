package com.mockerview.payment.gateway;

import com.mockerview.payment.dto.PaymentRequest;
import com.mockerview.payment.dto.PaymentResult;

public interface PaymentGateway {
    PaymentResult processPayment(PaymentRequest request);
    PaymentResult confirmPayment(String paymentKey, String orderId, Integer amount);
    PaymentResult cancelPayment(String orderId, String reason);
    String getProviderName();
}
