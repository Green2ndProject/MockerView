package com.mockerview.controller.web;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.dto.RegisterDTO;
import com.mockerview.entity.Answer;
import com.mockerview.entity.Session;
import com.mockerview.entity.User;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.SessionRepository;
import com.mockerview.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // ✅ 추가
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/auth")
@Slf4j
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private SessionRepository sessionRepository;
    
    @Autowired
    private AnswerRepository answerRepository;

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

    /**
     * ✅ 최적화: @AuthenticationPrincipal을 사용하여 DB 재조회 (username -> User) 과정을 ID 기반으로 변경
     */
    @GetMapping("/mypage")
    // Authentication 대신 @AuthenticationPrincipal로 CustomUserDetails를 바로 주입받습니다.
    public String mypage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        
        // 비로그인 사용자 처리
        if (userDetails == null) {
            return "redirect:/auth/login";
        }
        
        // 💡 userDetails에서 ID를 바로 가져옵니다. (DB 재조회 최소화)
        Long userId = userDetails.getUserId();

        // 💡 ID를 사용하여 DB에서 User 엔티티를 조회합니다. (최적화된 DB 조회)
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
        
            try {
                // 이하는 ID를 사용한 로직은 그대로 유지합니다.
                List<Session> hostedSessions = sessionRepository.findByHostId(userId);
                List<Answer> userAnswers = answerRepository.findByUserId(userId);
                
                long participatedSessionCount = 0;
                long answerCount = 0;
                
                if (userAnswers != null && !userAnswers.isEmpty()) {
                    answerCount = userAnswers.size();
                    try {
                        participatedSessionCount = userAnswers.stream()
                            .filter(a -> a.getQuestion() != null && a.getQuestion().getSession() != null)
                            .map(a -> a.getQuestion().getSession().getId())
                            .distinct()
                            .count();
                    } catch (Exception e) {
                        log.error("세션 카운트 중 오류: ", e);
                        participatedSessionCount = 0;
                    }
                }
                
                model.addAttribute("user", user);
                model.addAttribute("hostedSessions", hostedSessions != null ? hostedSessions : List.of());
                model.addAttribute("participatedSessionCount", participatedSessionCount);
                model.addAttribute("answerCount", answerCount);
                
                return "user/mypage";
                
            } catch (Exception e) {
                log.error("마이페이지 로드 오류: ", e);
                model.addAttribute("error", "페이지를 불러올 수 없습니다.");
                return "redirect:/session/list";
            }
        }

        // userDetails는 있지만 DB에서 사용자를 찾지 못한 경우
        return "redirect:/auth/login";
    }

    /**
     * ✅ 최적화: @AuthenticationPrincipal을 사용하여 DB 재조회 (username -> User) 과정을 ID 기반으로 변경
     */
    @PostMapping("/mypage/update")
    public String updateProfile(@RequestParam String name,
                                @RequestParam String email,
                                @RequestParam(required = false) String password,
                                // 💡 @AuthenticationPrincipal로 DTO를 바로 받습니다.
                                @AuthenticationPrincipal CustomUserDetails userDetails, 
                                RedirectAttributes redirectAttributes) {
        
        if (userDetails == null) {
            return "redirect:/auth/login";
        }
        
        // 💡 userDetails에서 ID를 바로 가져옵니다.
        Long userId = userDetails.getUserId();
        
        // 💡 ID를 사용하여 DB에서 User 엔티티를 조회합니다.
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // ... (업데이트 로직은 동일)
            User updatedUser = user.toBuilder()
                    .name(name)
                    .email(email)
                    .build();
            
            if (password != null && !password.trim().isEmpty()) {
                updatedUser = updatedUser.toBuilder()
                        .password(passwordEncoder.encode(password))
                        .build();
            }
            
            userRepository.save(updatedUser);
            redirectAttributes.addFlashAttribute("success", "프로필이 업데이트되었습니다.");
        } else {
            // ID는 있지만 DB에서 사용자를 못 찾은 경우
            redirectAttributes.addFlashAttribute("error", "사용자 정보를 찾을 수 없습니다.");
        }
        
        return "redirect:/auth/mypage";
    }
}