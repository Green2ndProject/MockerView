package com.mockerview.jwt;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.User;
import com.mockerview.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JWTFilter extends OncePerRequestFilter{

  private final JWTUtil jwtUtil;
  private final UserRepository userRepository;

  public JWTFilter(JWTUtil jwtUtil, UserRepository userRepository){
    this.jwtUtil = jwtUtil;
    this.userRepository = userRepository;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    
        String requestUri = request.getRequestURI();

        if (requestUri.startsWith("/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("[JWTFilter] Processing: {}", requestUri);

        String token = null;
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
          token = authorizationHeader.substring(7);
        }

        if(token == null){
          Cookie[] cookies = request.getCookies();
          if(cookies != null){
            for(Cookie cookie : cookies){
              if("Authorization".equals(cookie.getName())){
                token = cookie.getValue();
                log.debug("🍪 Token from cookie");
                break;
              }
            }
          } 
        }

        if(token == null){
            log.debug("No token - continuing as guest");
            filterChain.doFilter(request, response);
            return;
        }

        if (jwtUtil.isExpired(token)) {
          log.info("⏰ Token expired for: {}", requestUri);
          filterChain.doFilter(request, response);
          return;
        }

        String username = jwtUtil.getUsername(token);
        
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
          log.warn("⚠️ User not found: {}", username);
          filterChain.doFilter(request, response);
          return;
        }

        if (user.isDeleted()) {
          log.warn("⚠️ Deleted user access attempt: {}", username);
          filterChain.doFilter(request, response);
          return;
        }

        CustomUserDetails customUserDetails = new CustomUserDetails(user);

        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authToken);
        
        log.debug("✅ Auth OK: {}", username);

        filterChain.doFilter(request, response);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {

    String uri = request.getRequestURI();

    if (uri.equals("/auth/login") || uri.equals("/auth/register") || 
        uri.equals("/auth/find-username") || uri.equals("/auth/reset-password")) {
        return true;
    }

    if (uri.startsWith("/api/questions/categories")) {
        return true;
    }

    if (uri.equals("/manifest.json") ||
        uri.equals("/service-worker.js") ||
        uri.equals("/offline.html") ||
        uri.startsWith("/apple-touch-icon")) {
        return true;
    }

    if (uri.startsWith("/images/") ||
        uri.startsWith("/css/") ||
        uri.startsWith("/js/") ||
        uri.equals("/favicon.ico")) {
        return true;
    }
    
    return false;
  }
}