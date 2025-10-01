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
/*
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

  private final JWTUtil jwtUtil;

  public LoginFilter(JWTUtil jwtUtil){

          super();
          this.setFilterProcessesUrl("/auth/login");
          this.jwtUtil               = jwtUtil;

  }

  @Override
  public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
      throws AuthenticationException {

        if (!request.getMethod().equals("POST")) {

           throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());

        }

        try {
          
          InputStream inputStream = request.getInputStream();
          LoginDTO data = new ObjectMapper().readValue(inputStream, LoginDTO.class);

          String username = data.getUsername();
          String password = data.getPassword();
          
          UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password, null);

          setDetails(request, authToken);

          return this.getAuthenticationManager().authenticate(authToken);

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
        String token = jwtUtil.createJwt(username, role, 1000*60*60*3L);
        
        response.setHeader("Authorization", "Bearer " + token);
  }

  @Override
  protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
          AuthenticationException failed) throws IOException, ServletException {

      response.setStatus(401);
  }



  

}
*/

public class LoginFilter extends GenericFilterBean { 

    private final ObjectMapper objectMapper = new ObjectMapper(); 

    private final JWTUtil jwtUtil;
    // LoginFilter가 AuthenticationManager를 직접 사용하도록 필드 추가
    private final AuthenticationManager authenticationManager; 

    // 🚨 생성자 변경: AuthenticationManager를 받도록 변경
    public LoginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    // 🚨 doFilter 메서드를 오버라이드하여 필터링 로직을 직접 구현
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // 🚨 1. GET 요청 및 로그인 페이지 요청이면 인증 시도 없이 체인을 통과시키고 종료 (Controller로 이동)
        if (req.getMethod().equals("GET") && req.getRequestURI().equals("/auth/login")) {
            chain.doFilter(req, res);
            return; 
        }

        // 🚨 2. POST 요청 및 로그인 페이지 요청인 경우에만 인증 시도
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
              System.err.println("🚨 JSON Parsing Error or other Exception: " + e.getMessage()); // 🚨 이 로그를 추가하세요.
              res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
              res.setContentType("application/json;charset=UTF-8"); // 클라이언트 JSON 파싱 에러 방지
              res.getWriter().write("{\"error\": \"JSON Parsing Error\"}");
              res.getWriter().flush();
              return;
            }
        }
        
        // '/auth/login'이 아닌 다른 요청에 대해 다음 필터로 진행
        chain.doFilter(req, res);
    }
    
    // 🚨 successfulAuthentication 메서드 (유지)
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain,
        Authentication authentication) throws IOException, ServletException {
        // ... (기존 successfulAuthentication 로직 그대로 유지)
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

        String username = customUserDetails.getUsername();

        //String username = authentication.getName();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();

        GrantedAuthority auth = iterator.next();

        String role = auth.getAuthority();
        String token = jwtUtil.createJwt(username, role, 1000 * 60 * 60 * 3L);

        Cookie cookie = new Cookie("jwtToken", token);
        cookie.setMaxAge(3*60*60);
        cookie.setPath("/");
        cookie.setHttpOnly(true);

        response.addCookie(cookie);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\": true, \"message\": \"로그인 성공\"}");
        response.getWriter().flush();
        response.getWriter().close();

        System.out.println("✅ JWT Token 생성 성공: " + token + "(Cookie로 전송)"); 

        //response.setHeader("Authorization", "Bearer " + token);
    }

    // 🚨 unsuccessfulAuthentication 메서드 (유지)
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
        AuthenticationException failed) throws IOException, ServletException {
        // (기존 unsuccessfulAuthentication 로직 그대로 유지)
        response.setStatus(401); 
    }
}