package com.mockerview.controller.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    
    @GetMapping("/")
    public String index(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            return "redirect:/session/list";
        }
        return "index";
    }

    @GetMapping("/index")
    public String indexAlias(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            return "redirect:/session/list";
        }
        return "index";
    }

    @GetMapping("/home")
    public String home() {
        return "redirect:/session/list";
    }

    @GetMapping("/offline")
    public String offline() {
        return "offline";
    }

}