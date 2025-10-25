package com.mockerview.oauth;

import com.mockerview.entity.User;
import com.mockerview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        
        log.info("OAuth2 로그인 시도: provider={}", registrationId);

        String email;
        String name;

        if ("google".equals(registrationId)) {
            email = oAuth2User.getAttribute("email");
            name = oAuth2User.getAttribute("name");
        } else if ("kakao".equals(registrationId)) {
            Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            
            email = (String) kakaoAccount.get("email");
            name = (String) profile.get("nickname");
            
            log.info("카카오 로그인: email={}, nickname={}", email, name);
        } else {
            throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 provider: " + registrationId);
        }

        if (email == null) {
            log.error("이메일을 가져올 수 없음: provider={}", registrationId);
            throw new OAuth2AuthenticationException("이메일 정보가 없습니다");
        }

        log.info("OAuth2 로그인: provider={}, email={}, name={}", registrationId, email, name);

        User user = userRepository.findByEmailIncludingDeleted(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setUsername(email);
                    newUser.setName(name != null ? name : email);
                    newUser.setRole(User.UserRole.STUDENT);
                    newUser.setIsDeleted(0);
                    return userRepository.save(newUser);
                });

        if (user.getIsDeleted() == 1) {
            log.info("✅ 탈퇴한 유저 재가입: email={}", email);
            user.setIsDeleted(0);
            user.setDeletedAt(null);
            user.setWithdrawalReason(null);
            user.setName(name != null ? name : email);
            userRepository.save(user);
        }

        OAuth2UserDTO oauth2UserDTO = new OAuth2UserDTO();
        oauth2UserDTO.setUsername(user.getUsername());
        oauth2UserDTO.setName(user.getName());
        oauth2UserDTO.setEmail(user.getEmail());
        oauth2UserDTO.setRole("ROLE_" + user.getRole().name());

        return new CustomOAuth2User(oauth2UserDTO);
    }
}