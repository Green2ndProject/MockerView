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

        log.error("[JWTFilter] doFilterInternal started! This means shouldNotFilter was FALSE for URI: {}", request.getRequestURI());

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
                log.info("authorization now. Token extracted from Cookie: {}", token);
                break;
              }
            }
          } 
        }

        if(token == null){
            filterChain.doFilter(request, response);
            return;
        }

        log.info("authorization now. Token extracted: {}", token);

        if (jwtUtil.isExpired(token)) {
          System.out.println("토큰 만료");
          filterChain.doFilter(request, response);
          return;
        }

        String username = jwtUtil.getUsername(token);
        
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
          log.warn("Deleted or not found user tried to access: {}", username);
          filterChain.doFilter(request, response);
          return;
        }

        CustomUserDetails customUserDetails = new CustomUserDetails(user);

        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {

    String uri = request.getRequestURI();
    String method = request.getMethod();

    if (uri.equals("/") || uri.equals("/index")) {
        return true;
    }

    if (uri.equals("/auth/login")) {
        return true;
    }

    if (uri.equals("/auth/register")) {
        return true;
    }

    if (uri.equals("/auth/find-username")) {
        return true;
    }

    if (uri.equals("/auth/reset-password")) {
        return true;
    }

    if (uri.equals("/manifest.json") ||
        uri.equals("/service-worker.js") ||
        uri.equals("/offline.html") ||
        uri.startsWith("/apple-touch-icon")) {
        log.warn("[JWTFilter] Bypass! PWA file is skipping JWT validation: {}", uri);
        return true;
    }

    if (uri.startsWith("/images/") ||
        uri.startsWith("/css/") ||
        uri.startsWith("/js/") ||
        uri.equals("/favicon.ico")) {
            
        log.warn("[JWTFilter] Bypass! Static resource is skipping JWT validation: {}", uri); 
        return true;
    }
    
    log.info("[JWTFilter] Checking URI: {}", request.getRequestURI()); 
    return false;
  }
}