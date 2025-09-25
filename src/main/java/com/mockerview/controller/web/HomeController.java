package com.mockerview.controller.web;

import com.mockerview.entity.Session;
import com.mockerview.entity.User;
import com.mockerview.repository.SessionRepository;
import com.mockerview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class HomeController {
    
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    @GetMapping("/")
    public String home() {
        return "redirect:/session/list";
    }
    
    @PostMapping("/session/create")
    public String createSession(@RequestParam String title,
                                @RequestParam(defaultValue = "1") Long hostId,
                                RedirectAttributes redirectAttributes) {
        try {
            log.info("새 세션 생성 요청 - 제목: {}, 호스트 ID: {}", title, hostId);
            
            User host = userRepository.findById(hostId)
                .orElseThrow(() -> new RuntimeException("호스트를 찾을 수 없습니다: " + hostId));
            
            Session session = Session.builder()
                .title(title != null && !title.trim().isEmpty() ? title.trim() : "새 모의면접 세션")
                .host(host)
                .status(Session.SessionStatus.PLANNED)
                .createdAt(LocalDateTime.now())
                .build();
            
            Session savedSession = sessionRepository.save(session);
            log.info("세션 생성 완료 - ID: {}, 제목: {}", savedSession.getId(), savedSession.getTitle());
            
            redirectAttributes.addFlashAttribute("success", "새 세션이 생성되었습니다: " + savedSession.getTitle());
            
            return "redirect:/session/" + savedSession.getId() + "?userId=" + hostId + "&userName=" + host.getName();
            
        } catch (Exception e) {
            log.error("세션 생성 오류: ", e);
            redirectAttributes.addFlashAttribute("error", "세션 생성 실패: " + e.getMessage());
            return "redirect:/session/list";
        }
    }
}