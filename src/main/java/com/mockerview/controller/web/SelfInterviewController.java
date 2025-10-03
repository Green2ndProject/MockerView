package com.mockerview.controller.web;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.Session;
import com.mockerview.service.SelfInterviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/selfinterview")
@RequiredArgsConstructor
@Slf4j
public class SelfInterviewController {

    private final SelfInterviewService selfInterviewService;

    @GetMapping("/create")
    public String createPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("currentUser", userDetails);
        return "selfinterview/create";
    }

    @GetMapping("/list")
    public String listPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<Session> sessions = selfInterviewService.getUserSelfInterviews(userDetails.getUserId());
        model.addAttribute("sessions", sessions);
        model.addAttribute("currentUser", userDetails);
        return "selfinterview/list";
    }

    @GetMapping("/room/{id}")
    public String roomPage(@PathVariable Long id, 
                          @AuthenticationPrincipal CustomUserDetails userDetails, 
                          Model model) {
        model.addAttribute("currentUser", userDetails);
        return "selfinterview/room";
    }
}
