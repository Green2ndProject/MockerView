package com.mockerview.controller.web;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.Answer;
import com.mockerview.entity.Question;
import com.mockerview.entity.Session;
import com.mockerview.entity.User;
import com.mockerview.entity.User.UserRole;
import com.mockerview.repository.UserRepository;
import com.mockerview.service.SessionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/session")
@RequiredArgsConstructor
@Slf4j
public class SessionWebController {

    private final SessionService sessionService;
    private final UserRepository userRepository;

    /** 세션 참가: 역할 설정 후 세션 방으로 리다이렉트 */
    @GetMapping("/{sessionId}/join")
    public String joinSession(@PathVariable Long sessionId,
                              @RequestParam String role,
                              @AuthenticationPrincipal CustomUserDetails customUserDetails, // JWT 사용자 정보
                              HttpSession httpSession) { // 세션 역할 저장용

        if (customUserDetails == null) {
            return "redirect:/auth/login";
        }

        Long userId = customUserDetails.getUserId();

        User.UserRole selectedRole;
        try {
            selectedRole = User.UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            selectedRole = User.UserRole.STUDENT;
        }

        httpSession.setAttribute("sessionRole", selectedRole);
        return "redirect:/session/" + sessionId;
    }

    /** 세션 방 접속 (실제 모의면접 화면) */
    @GetMapping("/{sessionId}")
    public String sessionRoom(@PathVariable Long sessionId,
                              Model model,
                              @AuthenticationPrincipal CustomUserDetails customUserDetails, // JWT 사용자 정보
                              HttpSession httpSession) { // 세션 역할 로드용

        if (customUserDetails == null) {
            return "redirect:/auth/login";
        }

        Long userId = customUserDetails.getUserId();
        User.UserRole sessionRole = (User.UserRole) httpSession.getAttribute("sessionRole");

        if (sessionRole == null) {
            sessionRole = User.UserRole.STUDENT;
        }

        try {
            Session session = sessionService.findById(sessionId);
            if (session == null) {
                model.addAttribute("error", "세션을 찾을 수 없습니다.");
                return "error";
            }

            // DB에서 전체 사용자 엔티티 로드
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

            boolean isHost = sessionRole.equals(User.UserRole.HOST);

            model.addAttribute("sessionId", sessionId);
            model.addAttribute("sessionTitle", session.getTitle() != null ? session.getTitle() : "모의면접 세션");
            model.addAttribute("userId", userId);
            model.addAttribute("userName", user.getName());
            model.addAttribute("currentUser", user);
            model.addAttribute("isHost", isHost);

            return "session/session";

        } catch (Exception e) {
            log.error("세션 로드 오류: ", e);
            model.addAttribute("error", "세션을 불러올 수 없습니다: " + e.getMessage());
            return "error";
        }
    }

    /** 세션 목록 조회 */
    @GetMapping("/list")
    public String sessionList(Model model, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
            try {
            // 1. 모든 세션 목록 조회 (로그인 여부와 관계없이)
            List<Session> sessions = sessionService.getAllSessions();
            model.addAttribute("sessions", sessions);

            // 2. 로그인 사용자 정보 처리
            if (customUserDetails != null) {

                // ✅ 수정 로직: ID 대신 username을 사용하여 DB에서 User 엔티티를 조회합니다.
                // ID가 null인 문제를 우회하고, JWT에 확실히 존재하는 username을 사용합니다.
                String username = customUserDetails.getUsername(); 

                // UserRepository에 findByUsername 메서드가 정의되어 있어야 합니다.
                User currentUser = userRepository.findByUsername(username).orElse(null); 

                if (currentUser != null) {
                    model.addAttribute("currentUser", currentUser);
                    model.addAttribute("isLoggedIn", true);
                } else {
                    // 사용자 이름은 있지만 DB에서 사용자를 못 찾은 경우
                    model.addAttribute("currentUser", null);
                    model.addAttribute("isLoggedIn", false);
                }
            } else {
                // 비로그인 (customUserDetails가 null)인 경우
                model.addAttribute("currentUser", null);
                model.addAttribute("isLoggedIn", false);
            }

            return "session/list";

        } catch (Exception e) {
            log.error("세션 목록 로드 오류: ", e);
            model.addAttribute("error", "세션 목록을 불러올 수 없습니다: " + e.getMessage());
            return "session/list";
        }
    }

    /** 세션 생성 */
    @PostMapping("/create")
    public String createSession(@RequestParam String title,
                                @AuthenticationPrincipal CustomUserDetails customUserDetails) { // 호스트 ID 획득

        try {
            if (customUserDetails == null) {
                return "redirect:/auth/login";
            }

            Long hostId = customUserDetails.getUserId();

            sessionService.createSession(title, hostId);
            return "redirect:/session/list?success=세션이 생성되었습니다";
        } catch (Exception e) {
            log.error("세션 생성 오류: ", e);
            return "redirect:/session/list?error=" + e.getMessage();
        }
    }

    /** 세션 상세 정보 조회 */
    @GetMapping("/detail/{id}")
    public String sessionDetail(@PathVariable Long id,
                                Model model,
                                @AuthenticationPrincipal CustomUserDetails customUserDetails) { // 로그인 상태 확인용

        try {
            Session sess = sessionService.findById(id);
            List<Question> questions = sessionService.getSessionQuestions(id);
            List<Answer> answers = sessionService.getSessionAnswers(id);

            Map<Long, List<Answer>> answersByQuestion = answers.stream()
                .collect(Collectors.groupingBy(a -> a.getQuestion().getId()));

            User currentUser = null;
            if (customUserDetails != null) {
                Long userId = customUserDetails.getUserId();
                currentUser = userRepository.findById(userId).orElse(null);
            }

            model.addAttribute("interviewSession", sess);
            model.addAttribute("questions", questions);
            model.addAttribute("answersByQuestion", answersByQuestion);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("totalAnswerCount", answers.size());
            model.addAttribute("answeredQuestionCount", answersByQuestion.size());

            return "session/detail";

        } catch (Exception e) {
            log.error("세션 상세 조회 오류: ", e);
            return "redirect:/session/list";
        }
    }
}