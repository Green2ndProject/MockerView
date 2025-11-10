package com.mockerview.controller.api;

import com.mockerview.entity.User;
import com.mockerview.entity.UserBadge;
import com.mockerview.service.BadgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
public class BadgeApiController {

    private final BadgeService badgeService;

    @GetMapping("/my")
    public ResponseEntity<?> getMyBadges(@AuthenticationPrincipal User user) {
        List<UserBadge> badges = badgeService.getUserBadges(user);
        return ResponseEntity.ok(badges);
    }

    @GetMapping("/progress")
    public ResponseEntity<?> getBadgeProgress(@AuthenticationPrincipal User user) {
        Map<String, Object> progress = badgeService.getBadgeProgress(user);
        return ResponseEntity.ok(progress);
    }
}
