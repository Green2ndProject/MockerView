package com.mockerview.controller;

import com.mockerview.entity.Payment;
import com.mockerview.entity.Subscription;
import com.mockerview.entity.User;
import com.mockerview.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentService paymentService;
    private final SubscriptionService subscriptionService;
    private final UserService userService;
    
    @Value("${toss.client-key}")
    private String tossClientKey;
    
    @GetMapping("/plans")
    public String showPlans(Authentication authentication, Model model) {
        if (authentication != null) {
            User user = userService.findByUsername(authentication.getName());
            Subscription currentSub = subscriptionService.getActiveSubscription(user.getId());
            
            model.addAttribute("currentPlan", currentSub != null ? currentSub.getPlanType().name() : "NONE");
            model.addAttribute("usedSessions", currentSub != null ? currentSub.getUsedSessions() : 0);
            model.addAttribute("sessionLimit", currentSub != null ? currentSub.getSessionLimit() : 0);
        }
        
        return "payment/plans";
    }
    
    @GetMapping("/checkout-page")
    public String checkoutPage(
        @RequestParam String planType,
        @RequestParam(required = false) String method,
        Authentication authentication,
        Model model
    ) {
        User user = userService.findByUsername(authentication.getName());
        Subscription.PlanType plan = Subscription.PlanType.valueOf(planType);
        
        String orderId = plan.name() + "-" + java.util.UUID.randomUUID().toString();
        Integer amount = subscriptionService.getPlanPrice(plan);
        
        model.addAttribute("orderId", orderId);
        model.addAttribute("amount", amount);
        model.addAttribute("orderName", plan.name() + " 플랜");
        model.addAttribute("customerName", user.getName());
        model.addAttribute("customerEmail", user.getEmail());
        model.addAttribute("tossClientKey", tossClientKey);
        model.addAttribute("planType", planType);
        model.addAttribute("paymentMethod", method != null ? method : "CARD");
        
        return "payment/checkout";
    }
    
    @GetMapping("/success")
    public String paymentSuccess(
        @RequestParam String paymentKey,
        @RequestParam String orderId,
        @RequestParam Integer amount,
        Authentication authentication,
        Model model
    ) {
        try {
            log.info("결제 승인 시작 - orderId: {}, paymentKey: {}, amount: {}", orderId, paymentKey, amount);
            
            User user = userService.findByUsername(authentication.getName());
            
            paymentService.confirmPayment(paymentKey, orderId, amount);
            
            String planTypeStr = orderId.split("-")[0];
            Subscription.PlanType planType = Subscription.PlanType.valueOf(planTypeStr);
            
            Subscription subscription = subscriptionService.createSubscription(user.getId(), planType);
            
            model.addAttribute("message", "결제가 완료되었습니다!");
            model.addAttribute("orderId", orderId);
            model.addAttribute("amount", amount);
            model.addAttribute("planType", planType.name());
            
            log.info("결제 완료 - userId: {}, planType: {}, subscriptionId: {}", 
                user.getId(), planType, subscription.getId());
            
            return "payment/success";
        } catch (Exception e) {
            log.error("결제 승인 실패: ", e);
            String errorMessage = "결제 처리 중 오류가 발생했습니다.";
            if (e.getMessage() != null) {
                if (e.getMessage().contains("ALREADY_PROCESSED_PAYMENT")) {
                    errorMessage = "이미 처리된 결제입니다.";
                } else if (e.getMessage().contains("INVALID_")) {
                    errorMessage = "유효하지 않은 결제 정보입니다.";
                }
            }
            model.addAttribute("message", errorMessage);
            return "payment/fail";
        }
    }
    
    @GetMapping("/fail")
    public String paymentFail(
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String message,
        Model model
    ) {
        String errorMessage = "결제에 실패했습니다.";
        
        if (message != null && !message.isEmpty()) {
            errorMessage = message;
        } else if (code != null) {
            switch (code) {
                case "USER_CANCEL":
                    errorMessage = "사용자가 결제를 취소했습니다.";
                    break;
                case "INVALID_CARD_COMPANY":
                    errorMessage = "유효하지 않은 카드사입니다.";
                    break;
                case "EXCEED_MAX_CARD_INSTALLMENT_PLAN":
                    errorMessage = "할부 개월 수가 초과되었습니다.";
                    break;
                default:
                    errorMessage = "결제 중 오류가 발생했습니다. (코드: " + code + ")";
            }
        }
        
        model.addAttribute("message", errorMessage);
        return "payment/fail";
    }
    
    @GetMapping("/history")
    public String paymentHistory(Authentication authentication, Model model) {
        User user = userService.findByUsername(authentication.getName());
        model.addAttribute("payments", paymentService.getPaymentsByUserId(user.getId()));
        return "payment/history";
    }
}
