package com.mockerview.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.User;
import com.mockerview.repository.UserRepository;

public class CustomUserDetailsService implements UserDetailsService{

    private UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username);

        if(user != null){
            return new CustomUserDetails(user);
        }

        throw new UsernameNotFoundException("사용자 정보를 찾을 수 없습니다.");
    }


}
