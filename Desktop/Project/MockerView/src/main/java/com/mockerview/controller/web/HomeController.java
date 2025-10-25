package com.mockerview.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    
    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/index")
    public String indexAlias() {
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