package com.mockerview.controller;

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
        User user = userService.findByUsername(authentication.getName());
        Subscription currentSub = subscriptionService.getActiveSubscription(user.getId());
        
        model.addAttribute("currentPlan", currentSub != null ? currentSub.getPlanType().name() : "NONE");
        model.addAttribute("usedSessions", currentSub != null ? currentSub.getUsedSessions() : 0);
        model.addAttribute("sessionLimit", currentSub != null ? currentSub.getSessionLimit() : 0);
        
        return "payment/plans";
    }
    
    @PostMapping("/checkout")
    @ResponseBody
    public Map<String, Object> checkout(
        @RequestParam String planType,
        Authentication authentication
    ) {
        User user = userService.findByUsername(authentication.getName());
        Subscription.PlanType plan = Subscription.PlanType.valueOf(planType);
        
        String orderId = planType + "-" + paymentService.createOrder(user.getId(), plan);
        Integer amount = subscriptionService.getPlanPrice(plan);
        
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("amount", amount);
        result.put("orderName", plan.name() + " 플랜");
        result.put("customerName", user.getName());
        result.put("successUrl", "http://localhost:8080/payment/success");
        result.put("failUrl", "http://localhost:8080/payment/fail");
        
        return result;
    }
    
    @GetMapping("/checkout-page")
    public String checkoutPage(
        @RequestParam String planType,
        Authentication authentication,
        Model model
    ) {
        User user = userService.findByUsername(authentication.getName());
        Subscription.PlanType plan = Subscription.PlanType.valueOf(planType);
        
        String orderId = planType + "-" + paymentService.createOrder(user.getId(), plan);
        Integer amount = subscriptionService.getPlanPrice(plan);
        
        model.addAttribute("orderId", orderId);
        model.addAttribute("amount", amount);
        model.addAttribute("orderName", plan.name() + " 플랜");
        model.addAttribute("customerName", user.getName());
        model.addAttribute("tossClientKey", tossClientKey);
        
        return "payment/checkout";
    }
    
    @GetMapping("/success")
    public String paymentSuccess(
        @RequestParam String paymentKey,
        @RequestParam String orderId,
        @RequestParam Integer amount,
        Model model
    ) {
        try {
            paymentService.confirmPayment(paymentKey, orderId, amount);
            model.addAttribute("message", "결제가 완료되었습니다!");
            return "payment/success";
        } catch (Exception e) {
            log.error("결제 승인 실패: ", e);
            return "redirect:/payment/fail?message=" + e.getMessage();
        }
    }
    
    @GetMapping("/fail")
    public String paymentFail(@RequestParam String message, Model model) {
        model.addAttribute("message", message);
        return "payment/fail";
    }
}
