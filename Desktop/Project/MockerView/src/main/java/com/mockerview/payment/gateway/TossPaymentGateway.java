package com.mockerview.payment.gateway;

import com.mockerview.payment.dto.PaymentRequest;
import com.mockerview.payment.dto.PaymentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Base64;
import java.util.Map;

@Component
@Slf4j
public class TossPaymentGateway implements PaymentGateway {

    @Value("${toss.secret-key}")
    private String secretKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        return PaymentResult.builder()
            .success(true)
            .orderId(request.getOrderId())
            .message("토스 결제 요청 생성 완료")
            .build();
    }

    @Override
    public PaymentResult confirmPayment(String paymentKey, String orderId, Integer amount) {
        try {
            String auth = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + auth);
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> params = Map.of("paymentKey", paymentKey, "orderId", orderId, "amount", amount);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(params, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity("https://api.tosspayments.com/v1/payments/confirm", entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> body = response.getBody();
                return PaymentResult.builder().success(true).paymentKey(paymentKey).orderId(orderId).receiptUrl((String) body.get("receiptUrl")).message("결제 승인 완료").build();
            }
            return PaymentResult.builder().success(false).message("결제 승인 실패").build();
        } catch (Exception e) {
            log.error("토스 결제 승인 실패", e);
            return PaymentResult.builder().success(false).message("결제 승인 중 오류: " + e.getMessage()).build();
        }
    }

    @Override
    public PaymentResult cancelPayment(String orderId, String reason) {
        return PaymentResult.builder().success(true).message("결제 취소 완료").build();
    }

    @Override
    public String getProviderName() {
        return "TOSS";
    }
}
