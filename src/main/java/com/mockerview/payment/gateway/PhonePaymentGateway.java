package com.mockerview.payment.gateway;

import com.mockerview.payment.dto.PaymentRequest;
import com.mockerview.payment.dto.PaymentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PhonePaymentGateway implements PaymentGateway {

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        return PaymentResult.builder().success(true).orderId(request.getOrderId()).message("휴대폰 결제 요청").build();
    }

    @Override
    public PaymentResult confirmPayment(String paymentKey, String orderId, Integer amount) {
        return PaymentResult.builder().success(true).message("휴대폰 결제 승인").build();
    }

    @Override
    public PaymentResult cancelPayment(String orderId, String reason) {
        return PaymentResult.builder().success(true).message("휴대폰 결제 취소").build();
    }

    @Override
    public String getProviderName() {
        return "PHONE";
    }
}
