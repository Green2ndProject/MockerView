package com.mockerview.controller;

import com.mockerview.entity.Subscription;
import com.mockerview.entity.User;
import com.mockerview.service.SubscriptionService;
import com.mockerview.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/subscription")
@RequiredArgsConstructor
public class SubscriptionController {
    
    private final SubscriptionService subscriptionService;
    private final UserService userService;
    
    @GetMapping("/upgrade")
    public String upgradePage(Authentication authentication, Model model) {
        if (authentication != null) {
            User user = userService.findByUsername(authentication.getName());
            Subscription currentSub = subscriptionService.getActiveSubscription(user.getId());
            
            model.addAttribute("currentPlan", currentSub != null ? currentSub.getPlanType().name() : "FREE");
            model.addAttribute("usedSessions", currentSub != null ? currentSub.getUsedSessions() : 0);
            model.addAttribute("sessionLimit", currentSub != null ? currentSub.getSessionLimit() : 0);
            model.addAttribute("usedReviews", currentSub != null ? currentSub.getUsedReviewReads() : 0);
            model.addAttribute("reviewLimit", currentSub != null ? currentSub.getReviewReadLimit() : 0);
        }
        
        return "payment/plans";
    }
}
