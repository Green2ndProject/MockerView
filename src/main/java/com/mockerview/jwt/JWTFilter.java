package com.mockerview.jwt;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.User;
import com.mockerview.entity.User.UserRole;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JWTFilter extends OncePerRequestFilter{

  private final JWTUtil jwtUtil;

  public JWTFilter(JWTUtil jwtUtil){
    this.jwtUtil = jwtUtil;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    
        String authorization = request.getHeader("Authorization");

        if(authorization == null || !authorization.startsWith("Bearer ")){

          System.out.println("token null");
          filterChain.doFilter(request, response);

          return; 
        }

        System.out.println("authorization now");

        String token = authorization.split(" ")[1];

        if(jwtUtil.isExpired(token)){
          System.out.println("토큰 만료");
          filterChain.doFilter(request, response);

          return;
        }

        String username   = jwtUtil.getUsername(token);
        String roleString = jwtUtil.getRole(token);
        UserRole roleEnum = UserRole.valueOf(roleString);

        User user = new User();
        user.setUsername(username);
        user.setRole(roleEnum);
        user.setPassword("temppass");

        CustomUserDetails customUserDetails = new CustomUserDetails(user);

        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
  }
}




