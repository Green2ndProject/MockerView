package com.mockerview.jwt;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockerview.dto.CustomUserDetails;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class LoginFilter extends UsernamePasswordAuthenticationFilter {

  private final AuthenticationManager authenticationManager;

  private final JWTUtil jwtUtil;

  public LoginFilter(AuthenticationManager authenticationManager,
                     JWTUtil jwtUtil){
          this.authenticationManager = authenticationManager;
          this.jwtUtil               = jwtUtil;
  }

  @Override
  public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
      throws AuthenticationException {

        try {
          
          InputStream inputStream = request.getInputStream();
          LoginDTO data = new ObjectMapper().readValue(inputStream, LoginDTO.class);

          String username = data.getUsername();
          String password = data.getPassword();
          
          UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password, null);

          return authenticationManager.authenticate(authToken);

        } catch (Exception e) {

          throw new AuthenticationServiceException("JSON 파싱 오류");
        }

        

  }

  @Override
  protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain,
      Authentication authentication) throws IOException, ServletException {

        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

        String username = customUserDetails.getUsername();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();

        GrantedAuthority auth = iterator.next();

        String role  = auth.getAuthority();
        String token = jwtUtil.createJwt(username, role, 60*60*10L);
        
        response.setHeader("Authorization", "Bearer " + token);
  }

  @Override
  protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
          AuthenticationException failed) throws IOException, ServletException {

      response.setStatus(401);
  }



  

}
