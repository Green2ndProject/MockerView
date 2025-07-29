package com.mydiet.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TestController {
    
    @GetMapping("/test")
    public String test() {
        return "MyDiet API 작동 중! 🍎";
    }
}
