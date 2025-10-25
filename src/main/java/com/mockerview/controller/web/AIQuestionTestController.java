package com.mockerview.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AIQuestionTestController {
    
    @GetMapping("/ai-question-test")
    public String aiQuestionTest() {
        return "ai-question-test";
    }
}
