package com.mockerview.service;

import com.mockerview.entity.*;
import com.mockerview.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${toss.client-key:test_ck_D5GePWvyJnrK0W0k6q8gLzN97Eoq}")
    private String tossClientKey;
    
    @Value("${toss.secret-key:test_sk_zXLkKEypNArWmo50nX3lmeaxYG5R}")
    private String tossSecretKey;
    
    @Transactional
    public String createOrder(Long userId, Subscription.PlanType planType) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        String orderId = UUID.randomUUID().toString();
        Integer amount = subscriptionService.getPlanPrice(planType);
        
        Payment payment = Payment.builder()
            .user(user)
            .orderId(orderId)
            .method(Payment.PaymentMethod.CARD)
            .status(Payment.PaymentStatus.PENDING)
            .amount(BigDecimal.valueOf(amount))
            .currency("KRW")
            .requestedAt(LocalDateTime.now())
            .build();
        
        paymentRepository.save(payment);
        
        log.info("주문 생성 - orderId: {}, userId: {}, plan: {}, amount: {}", 
            orderId, userId, planType, amount);
        
        return orderId;
    }
    
    @Transactional
    public void confirmPayment(String paymentKey, String orderId, Integer amount) {
        try {
            String auth = Base64.getEncoder()
                .encodeToString((tossSecretKey + ":").getBytes());
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + auth);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> params = Map.of(
                "paymentKey", paymentKey,
                "orderId", orderId,
                "amount", amount
            );
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(params, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.tosspayments.com/v1/payments/confirm",
                request,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Payment payment = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new RuntimeException("Payment not found"));
                
                payment.setPaymentKey(paymentKey);
                payment.setStatus(Payment.PaymentStatus.COMPLETED);
                payment.setApprovedAt(LocalDateTime.now());
                paymentRepository.save(payment);
                
                String planTypeStr = orderId.split("-")[0];
                Subscription.PlanType planType = Subscription.PlanType.valueOf(planTypeStr);
                
                subscriptionService.upgradePlan(payment.getUser(), planType);
                
                log.info("결제 승인 완료 - orderId: {}, paymentKey: {}", orderId, paymentKey);
            }
            
        } catch (Exception e) {
            log.error("결제 승인 실패 - orderId: {}, error: {}", orderId, e.getMessage());
            
            Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
            if (payment != null) {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                payment.setFailedAt(LocalDateTime.now());
                payment.setFailureReason(e.getMessage());
                paymentRepository.save(payment);
            }
            
            throw new RuntimeException("결제 승인 실패: " + e.getMessage());
        }
    }
    
    @Transactional
    public void cancelPayment(String orderId, String reason) {
        Payment payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        payment.setStatus(Payment.PaymentStatus.CANCELLED);
        payment.setFailedAt(LocalDateTime.now());
        payment.setFailureReason(reason);
        paymentRepository.save(payment);
        
        log.info("결제 취소 - orderId: {}, reason: {}", orderId, reason);
    }
    
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserIdOrderByRequestedAtDesc(userId);
    }
}
