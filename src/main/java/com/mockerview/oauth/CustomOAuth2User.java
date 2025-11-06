package com.mockerview.oauth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User {

    private final OAuth2UserDTO oauth2UserDTO;

    @Override
    public Map<String, Object> getAttributes() {
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oauth2UserDTO.getAuthorities();
    }

    @Override
    public String getName() {
        return oauth2UserDTO.getName();
    }

    public String getUsername() {
        return oauth2UserDTO.getUsername();
    }

    public String getRole() {
        return oauth2UserDTO.getRole();
    }
}
