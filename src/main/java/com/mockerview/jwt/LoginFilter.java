package com.mockerview.jwt;

import java.io.BufferedReader;
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
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.GenericFilterBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockerview.dto.CustomUserDetails;
import com.mockerview.dto.LoginDTO;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class LoginFilter extends GenericFilterBean { 

    private final ObjectMapper objectMapper = new ObjectMapper(); 

    private final JWTUtil jwtUtil;
    private final AuthenticationManager authenticationManager; 

    public LoginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if (req.getMethod().equals("GET") && req.getRequestURI().equals("/auth/login")) {
            chain.doFilter(req, res);
            return; 
        }

        if (req.getMethod().equals("POST") && req.getRequestURI().equals("/auth/login")) {
            
            try {
                // 기존 attemptAuthentication 내부 로직 복사
                String jsonBody = StreamUtils.copyToString(req.getInputStream(), java.nio.charset.StandardCharsets.UTF_8);

                System.out.println("✅ Raw JSON Body: " + jsonBody); 

                if (jsonBody.isEmpty()) {
                    throw new java.io.IOException("Request body is empty.");
                }

                LoginDTO data = new ObjectMapper().readValue(jsonBody, LoginDTO.class);

                System.out.println("Json 파싱성공 username : " + data.getUsername());
                String username = data.getUsername();
                String password = data.getPassword();

                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(username, password, null);

                // AuthenticationManager를 사용하여 인증 시도
                Authentication authentication = authenticationManager.authenticate(authToken);

                // 인증 성공 처리
                successfulAuthentication(req, res, chain, authentication);
                return; // 성공했으니 여기서 필터 체인 종료
                
            } catch (AuthenticationException e) {
                // 인증 실패 처리
                unsuccessfulAuthentication(req, res, e);
                return; // 실패했으니 여기서 필터 체인 종료
            } catch (Exception e) {
              System.err.println("🚨 JSON Parsing Error or other Exception: " + e.getMessage()); // 이 로그 추가염
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.setContentType("application/json;charset=UTF-8"); // 클라이언트 JSON 파싱 에러 방지
                res.getWriter().write("{\"error\": \"JSON Parsing Error\"}");
                res.getWriter().flush();
                return;
            }
        }
        
        chain.doFilter(req, res);
    }
    
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain,
        Authentication authentication) throws IOException, ServletException {
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

        String username = customUserDetails.getUsername();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();

        GrantedAuthority auth = iterator.next();

        String role = auth.getAuthority();
        String token = jwtUtil.createJwt(username, role, 1000 * 60 * 60 * 3L);

        // 수정함: JavaScript에서 쿠키를 읽을 수 있도록 HttpOnly를 false로 변경해씀
        Cookie cookie = new Cookie("Authorization", token);
        cookie.setMaxAge(3*60*60);
        cookie.setPath("/");
        cookie.setHttpOnly(false); // true -> false로 변경! JavaScript에서 쿠키 읽어야댐

        response.addCookie(cookie);

        try {
            response.setStatus(HttpServletResponse.SC_OK); // 200 OK
            response.setContentType("application/json;charset=UTF-8");

            String jsonResponse = "{\"success\": true, \"redirectUrl\": \"/session/list\"}";
            response.getWriter().write(jsonResponse);
            
            return;

        } catch (IOException e) {
            throw new RuntimeException("Error writing JSON response", e);
        }
        
    
    }

    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
        AuthenticationException failed) throws IOException, ServletException {
        response.setStatus(401); 
    }
}