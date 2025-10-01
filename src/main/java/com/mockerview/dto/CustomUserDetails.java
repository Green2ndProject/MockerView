package com.mockerview.dto;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.mockerview.entity.User;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user){
        this.user = user;
    }

    /**
     * ✅ 추가된 메서드: User 엔티티의 ID를 반환
     */
    public Long getUserId() {
        return user.getId();
    }

    /**
     * ✅ 추가된 메서드 (선택 사항): User의 이름을 반환
     */
    public String getName() {
        return user.getName();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> collection = new ArrayList<>();
        
        // role은 enum객체, String 으로 변환
        // String roleString = user.getRole().name(); // 사용하지 않는 변수는 제거
        collection.add(new SimpleGrantedAuthority("ROLE_"+user.getRole().toString()));

        return collection;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        // UserDetails에서 Username은 보통 로그인 ID(email, loginId)를 의미합니다.
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}