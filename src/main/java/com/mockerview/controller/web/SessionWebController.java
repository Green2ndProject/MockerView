package com.mockerview.controller.web;

import com.mockerview.dto.SessionSearchDTO;
import com.mockerview.entity.Session;
import com.mockerview.entity.User;
import com.mockerview.repository.UserRepository;
import com.mockerview.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/session")
@RequiredArgsConstructor
@Slf4j
public class SessionWebController {
    
    private final SessionService sessionService;
    private final UserRepository userRepository;

    @GetMapping("/{sessionId}")
    public String sessionRoom(@PathVariable Long sessionId, 
                            @RequestParam(defaultValue = "1") Long userId,
                            @RequestParam(defaultValue = "테스트사용자") String userName,
                            Model model,
                            HttpSession httpSession) {
        
        try {
            log.info("세션 접속 - sessionId: {}, userId: {}, userName: {}", sessionId, userId, userName);
            
            Session session = sessionService.findById(sessionId);
            if (session == null) {
                model.addAttribute("error", "세션을 찾을 수 없습니다.");
                return "error";
            }
            
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
            
            boolean isHost = session.getHost() != null && session.getHost().getId().equals(userId);
            
            if (isHost && !user.getRole().equals(User.UserRole.HOST)) {
                log.info("사용자 {}를 HOST로 역할 변경", userId);
                user.setRole(User.UserRole.HOST);
                userRepository.save(user);
            }
            
            model.addAttribute("sessionId", sessionId);
            model.addAttribute("sessionTitle", session.getTitle() != null ? session.getTitle() : "모의면접 세션");
            model.addAttribute("userId", userId);
            model.addAttribute("userName", user.getName());
            model.addAttribute("currentUser", user);
            model.addAttribute("isHost", isHost);
            
            log.info("세션 로드 완료 - 사용자: {}, 역할: {}, 호스트여부: {}", 
                user.getName(), user.getRole(), isHost);
            
                return "session/session";
            
        } catch (Exception e) {
            log.error("세션 로드 오류 - sessionId: {}, userId: {}: ", sessionId, userId, e);
            model.addAttribute("error", "세션을 불러올 수 없습니다: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/list")
    public String sessionList(@ModelAttribute SessionSearchDTO searchDTO, Model model, HttpSession httpSession) {
        try {
            log.info("세션 목록 로드 중... 검색조건: {}", searchDTO);
            
            List<Session> sessions;
            
            if (searchDTO.getKeyword() != null || searchDTO.getStatus() != null || 
                searchDTO.getSortBy() != null || searchDTO.getSortOrder() != null) {
                sessions = sessionService.searchSessions(
                    searchDTO.getKeyword(),
                    searchDTO.getStatus(),
                    searchDTO.getSortBy(),
                    searchDTO.getSortOrder()
                );
                log.info("검색 실행 - 결과: {}개", sessions.size());
            } else {
                sessions = sessionService.getAllSessions();
                log.info("전체 목록 조회 - 결과: {}개", sessions.size());
            }
            
            model.addAttribute("sessions", sessions);
            model.addAttribute("searchDTO", searchDTO);
            
            Long userId = (Long) httpSession.getAttribute("userId");
            String userName = (String) httpSession.getAttribute("userName");
            
            if (userId != null && userName != null) {
                User currentUser = userRepository.findById(userId).orElse(null);
                model.addAttribute("currentUser", currentUser);
                model.addAttribute("isLoggedIn", true);
            } else {
                model.addAttribute("currentUser", null);
                model.addAttribute("isLoggedIn", false);
            }
            
            log.info("세션 목록 로드 완료 - {}개 세션", sessions.size());
            return "session/list";
            
        } catch (Exception e) {
            log.error("세션 목록 로드 오류: ", e);
            model.addAttribute("error", "세션 목록을 불러올 수 없습니다: " + e.getMessage());
            model.addAttribute("sessions", List.of());
            model.addAttribute("searchDTO", searchDTO);
            return "session/list";
        }
    }

    @PostMapping("/create")
    public String createSession(@RequestParam String title, 
                                @RequestParam Long hostId,
                                HttpSession httpSession) {
        try {
            log.info("세션 생성 요청 - title: {}, hostId: {}", title, hostId);
            sessionService.createSession(title, hostId);
            log.info("세션 생성 완료");
            return "redirect:/session/list?success=세션이 생성되었습니다";
        } catch (Exception e) {
            log.error("세션 생성 오류: ", e);
            return "redirect:/session/list?error=" + e.getMessage();
        }
    }
}
