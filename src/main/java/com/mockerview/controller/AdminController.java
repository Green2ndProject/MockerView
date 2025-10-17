package com.mockerview.controller;

import com.mockerview.entity.Subscription;
import com.mockerview.entity.User;
import com.mockerview.repository.PaymentRepository;
import com.mockerview.repository.SubscriptionRepository;
import com.mockerview.repository.UserRepository;
import com.mockerview.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionService subscriptionService;
    
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<User> allUsers = userRepository.findAll();
        List<Subscription> allSubscriptions = subscriptionRepository.findAll();
        
        long totalUsers = allUsers.size();
        long activeSubscriptions = subscriptionRepository.findByStatus(Subscription.SubscriptionStatus.ACTIVE).size();
        long freeUsers = allSubscriptions.stream()
            .filter(s -> s.getPlanType() == Subscription.PlanType.FREE)
            .count();
        long paidUsers = allSubscriptions.stream()
            .filter(s -> s.getPlanType() != Subscription.PlanType.FREE)
            .count();
        
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("activeSubscriptions", activeSubscriptions);
        model.addAttribute("freeUsers", freeUsers);
        model.addAttribute("paidUsers", paidUsers);
        model.addAttribute("users", allUsers);
        
        return "admin/dashboard";
    }
    
    @GetMapping("/users")
    public String userList(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "admin/users";
    }
    
    @GetMapping("/users/{userId}")
    public String userDetail(@PathVariable Long userId, Model model) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Subscription subscription = subscriptionService.getActiveSubscription(userId);
        
        model.addAttribute("user", user);
        model.addAttribute("subscription", subscription);
        
        return "admin/user-detail";
    }
    
    @PostMapping("/users/{userId}/change-plan")
    public String changePlan(
        @PathVariable Long userId,
        @RequestParam String planType,
        RedirectAttributes redirectAttributes
    ) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Subscription.PlanType newPlan = Subscription.PlanType.valueOf(planType);
            subscriptionService.createSubscription(userId, newPlan);
            
            log.info("관리자가 사용자 플랜 변경 - userId: {}, newPlan: {}", userId, newPlan);
            
            redirectAttributes.addFlashAttribute("success", "플랜이 변경되었습니다.");
            return "redirect:/admin/users/" + userId;
        } catch (Exception e) {
            log.error("플랜 변경 실패", e);
            redirectAttributes.addFlashAttribute("error", "플랜 변경에 실패했습니다.");
            return "redirect:/admin/users/" + userId;
        }
    }
    
    @GetMapping("/subscriptions")
    public String subscriptionList(Model model) {
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        model.addAttribute("subscriptions", subscriptions);
        return "admin/subscriptions";
    }
}
