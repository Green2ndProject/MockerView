package com.mockerview.controller.web;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.dto.RegisterDTO;
import com.mockerview.entity.User;
import com.mockerview.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/auth")
@Slf4j
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String loginForm() {
        log.info("로그인폼 controller 진입 성공!");
        return "user/login";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "user/register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute RegisterDTO registerDTO,
                            RedirectAttributes redirectAttributes) {
        
        String username = registerDTO.getUsername();
        String password = registerDTO.getPassword();
        String name = registerDTO.getName();
        String email = registerDTO.getEmail();
        String role = registerDTO.getRole();

        if (userRepository.findByUsername(username).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "이미 존재하는 아이디입니다.");
            return "redirect:/auth/register";
        }
        
        User user = User.builder()
                        .username(username)
                        .password(passwordEncoder.encode(password))
                        .name(name)
                        .email(email)
                        .role(User.UserRole.valueOf(role))
                        .build();
        
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", "회원가입이 완료되었습니다.");
        return "redirect:/auth/login";
    }

    @GetMapping("/withdraw")
    public String withdrawPage() {
        return "user/withdraw";
    }
}