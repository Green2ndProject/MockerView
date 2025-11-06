package com.mockerview.payment.gateway;

import com.mockerview.payment.dto.PaymentRequest;
import com.mockerview.payment.dto.PaymentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class VirtualAccountGateway implements PaymentGateway {

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        return PaymentResult.builder().success(true).orderId(request.getOrderId()).virtualAccountNumber("110-234-567890").virtualAccountBank("국민은행").message("가상계좌 발급 완료").build();
    }

    @Override
    public PaymentResult confirmPayment(String paymentKey, String orderId, Integer amount) {
        return PaymentResult.builder().success(true).message("가상계좌 입금 확인").build();
    }

    @Override
    public PaymentResult cancelPayment(String orderId, String reason) {
        return PaymentResult.builder().success(true).message("가상계좌 취소").build();
    }

    @Override
    public String getProviderName() {
        return "VIRTUAL_ACCOUNT";
    }
}
