package com.mockerview.service;

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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = extractEmail(registrationId, attributes);
        String name = extractName(registrationId, attributes);
        String providerId = extractProviderId(registrationId, attributes);
        User user = userRepository.findByUsername(registrationId + "_" + providerId).orElseGet(() -> {
            User newUser = User.builder().username(registrationId + "_" + providerId).password(UUID.randomUUID().toString()).name(name).email(email).role(User.UserRole.STUDENT).build();
            return userRepository.save(newUser);
        });
        return new com.mockerview.dto.CustomOAuth2User(user, attributes, userNameAttributeName);
    }

    private String extractEmail(String registrationId, Map<String, Object> attributes) {
        switch (registrationId) {
            case "google": return (String) attributes.get("email");
            case "kakao": return (String) ((Map<String, Object>) attributes.get("kakao_account")).get("email");
            case "naver": return (String) ((Map<String, Object>) attributes.get("response")).get("email");
            default: return null;
        }
    }

    private String extractName(String registrationId, Map<String, Object> attributes) {
        switch (registrationId) {
            case "google": return (String) attributes.get("name");
            case "kakao": return (String) ((Map<String, Object>) attributes.get("properties")).get("nickname");
            case "naver": return (String) ((Map<String, Object>) attributes.get("response")).get("name");
            default: return "Unknown";
        }
    }

    private String extractProviderId(String registrationId, Map<String, Object> attributes) {
        switch (registrationId) {
            case "google": return (String) attributes.get("sub");
            case "kakao": return String.valueOf(attributes.get("id"));
            case "naver": return (String) ((Map<String, Object>) attributes.get("response")).get("id");
            default: return null;
        }
    }
}
