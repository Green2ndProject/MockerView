package com.mockerview.service;

import java.util.Optional;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.User;
import com.mockerview.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService{

    private UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        // isDeleted 검증
        if(user.getIsDeleted() == 1){
            throw new DisabledException("탈퇴한 회원이거나 접근 권한이 없습니다.");
        }

        return new CustomUserDetails(user);
    }


}
