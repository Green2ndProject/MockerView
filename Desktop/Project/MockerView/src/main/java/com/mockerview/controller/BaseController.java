package com.mockerview.controller;

import com.mockerview.entity.Subscription;
import com.mockerview.entity.User;
import com.mockerview.service.SubscriptionService;
import com.mockerview.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class BaseController {
    
    private final UserService userService;
    private final SubscriptionService subscriptionService;
    
    @ModelAttribute("currentPlanType")
    public String getCurrentPlanType(Authentication authentication) {
        if (authentication == null) {
            return "FREE";
        }
        
        try {
            User user = userService.findByUsername(authentication.getName());
            Subscription subscription = subscriptionService.getActiveSubscription(user.getId());
            return subscription != null ? subscription.getPlanType().name() : "FREE";
        } catch (Exception e) {
            return "FREE";
        }
    }
    
    @ModelAttribute("isAdmin")
    public boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        
        try {
            User user = userService.findByUsername(authentication.getName());
            return user.getRole() == User.UserRole.ADMIN;
        } catch (Exception e) {
            return false;
        }
    }
}
