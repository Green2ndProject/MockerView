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

    // @PostMapping("/login")
    // public String login(@RequestParam String username, 
    //                    @RequestParam String password,
    //                    HttpSession session,
    //                    RedirectAttributes redirectAttributes) {
        
    //     Optional<User> userOpt = userRepository.findByUsername(username);
        
    //     if (userOpt.isPresent()) {
    //         User user = userOpt.get();
    //         if (user.getPassword() != null && passwordEncoder.matches(password, user.getPassword())) {
    //             session.setAttribute("userId", user.getId());
    //             session.setAttribute("userName", user.getName());
    //             session.setAttribute("userRole", user.getRole());
    //             return "redirect:/session/list";
    //         }
    //     }
        
    //     redirectAttributes.addFlashAttribute("error", "아이디 또는 비밀번호가 잘못되었습니다.");
    //     return "redirect:/auth/login";
    // }

    @GetMapping("/register")
    public String registerForm() {
        return "user/register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute RegisterDTO registerDTO,
                            RedirectAttributes redirectAttributes) {
        
        String username = registerDTO.getUsername();
        String password = registerDTO.getPassword();
        String name     = registerDTO.getName();
        String email    = registerDTO.getEmail();
        String role     = registerDTO.getRole();

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

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/auth/login";
    }

    // @GetMapping("/mypage")
    // public String mypage(HttpSession session, Model model) {
    //     Long userId = (Long) session.getAttribute("userId");
    //     if (userId == null) {
    //         return "redirect:/auth/login";
    //     }
        
    //     Optional<User> userOpt = userRepository.findById(userId);
    //     if (userOpt.isPresent()) {
    //         model.addAttribute("user", userOpt.get());
    //         return "user/mypage";
    //     }
        
    //     return "redirect:/auth/login";
    // }

    @GetMapping("/mypage")
    public String mypage(Authentication authentication, Model model) {
        
        if(authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)){
            return "redirect:/auth/login";
        }
        
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        String username = userDetails.getUsername();

        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent()) {
            model.addAttribute("user", userOpt.get());
            return "user/mypage";
        
        // try {
        //     Optional<User> userOpt = userRepository.findById(userId);
        //     if (userOpt.isPresent()) {
        //         User user = userOpt.get();
                
        //         List<Session> hostedSessions = sessionRepository.findByHostId(userId);
        //         List<Answer> userAnswers = answerRepository.findByUserId(userId);
                
        //         long participatedSessionCount = 0;
        //         long answerCount = 0;
                
        //         if (userAnswers != null && !userAnswers.isEmpty()) {
        //             answerCount = userAnswers.size();
        //             try {
        //                 participatedSessionCount = userAnswers.stream()
        //                     .filter(a -> a.getQuestion() != null && a.getQuestion().getSession() != null)
        //                     .map(a -> a.getQuestion().getSession().getId())
        //                     .distinct()
        //                     .count();
        //             } catch (Exception e) {
        //                 log.error("세션 카운트 중 오류: ", e);
        //                 participatedSessionCount = 0;
        //             }
        //         }
                
        //         model.addAttribute("user", user);
        //         model.addAttribute("hostedSessions", hostedSessions != null ? hostedSessions : List.of());
        //         model.addAttribute("participatedSessionCount", participatedSessionCount);
        //         model.addAttribute("answerCount", answerCount);
                
        //         return "user/mypage";
        //     }
            
        //     return "redirect:/auth/login";
            
        // } catch (Exception e) {
        //     log.error("마이페이지 로드 오류: ", e);
        //     model.addAttribute("error", "페이지를 불러올 수 없습니다.");
        //     return "redirect:/session/list";
        }

        return "redirect:/session/list";
    }

    @PostMapping("/mypage/update")
    public String updateProfile(@RequestParam String name,
                                @RequestParam String email,
                                @RequestParam(required = false) String password,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
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
            session.setAttribute("userName", updatedUser.getName());
            redirectAttributes.addFlashAttribute("success", "프로필이 업데이트되었습니다.");
        }
        
        return "redirect:/auth/mypage";
    }
}