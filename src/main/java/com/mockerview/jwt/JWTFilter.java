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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JWTFilter extends OncePerRequestFilter{

  private final JWTUtil jwtUtil;

  public JWTFilter(JWTUtil jwtUtil){
    this.jwtUtil = jwtUtil;
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
          // 토큰이 없는 건 정상적인 상황일 수 있으므로 (로그인 이전, 정적 파일 요청 등)
          // System.out.println("token null"); 같은 혼동을 주는 출력 대신 log.debug()를 사용하거나 제거합니다.
        
          token = authorizationHeader.substring(7);
          
        }

        if(token == null){
          Cookie[] cookies = request.getCookies();
          if(cookies != null){
            for(Cookie cookie : cookies){
              if("jwtToken".equals(cookie.getName())){
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

        String username   = jwtUtil.getUsername(token);
        String roleString = jwtUtil.getRole(token);
        String enumRoleName = roleString.startsWith("ROLE_") ? roleString.substring(5) : roleString;
        UserRole roleEnum = UserRole.valueOf(enumRoleName);

        User user = new User();
        user.setUsername(username);
        user.setRole(roleEnum);
        user.setPassword("temppass");

        CustomUserDetails customUserDetails = new CustomUserDetails(user);

        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {

    String uri    = request.getRequestURI();
    String method = request.getMethod();

    if (uri.equals("/auth/login") && method.equals("GET")) {
        return true;
    }

    if (uri.equals("/auth/register") && method.equals("GET")) {
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




