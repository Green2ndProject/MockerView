package com.mockerview.payment.gateway;

import com.mockerview.payment.dto.PaymentRequest;
import com.mockerview.payment.dto.PaymentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EasyPayGateway implements PaymentGateway {

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        return PaymentResult.builder().success(true).orderId(request.getOrderId()).message("간편결제 요청").build();
    }

    @Override
    public PaymentResult confirmPayment(String paymentKey, String orderId, Integer amount) {
        return PaymentResult.builder().success(true).message("간편결제 승인").build();
    }

    @Override
    public PaymentResult cancelPayment(String orderId, String reason) {
        return PaymentResult.builder().success(true).message("간편결제 취소").build();
    }

    @Override
    public String getProviderName() {
        return "EASY_PAY";
    }
}
