package com.mockerview.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.mockerview.jwt.JWTFilter;
import com.mockerview.jwt.JWTLogoutHandler;
import com.mockerview.jwt.JWTUtil;
import com.mockerview.jwt.LoginFilter;
import com.mockerview.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JWTLogoutHandler jwtLogoutHandler;
    private final JWTUtil jwtUtil;
    private final AuthenticationConfiguration authenticationConfiguration;

    public SecurityConfig(JWTUtil jwtUtil, 
                          AuthenticationConfiguration authenticationConfiguration,
                          JWTLogoutHandler jwtLogoutHandler
                         ) throws Exception {
        this.jwtUtil                     = jwtUtil;
        this.authenticationConfiguration = authenticationConfiguration;
        this.jwtLogoutHandler            = jwtLogoutHandler;
   
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(
                CustomUserDetailsService customUserDetailsService, 
                PasswordEncoder passwordEncoder  ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
                DaoAuthenticationProvider authenticationProvider ) throws Exception {
    
        return new ProviderManager(authenticationProvider);
    }


    @Bean
    public LoginFilter loginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil) throws Exception {
        
        LoginFilter filter = new LoginFilter(authenticationManager, jwtUtil);
        
        // filter.setAuthenticationManager(authenticationManager); 
    
        // filter.setFilterProcessesUrl("/auth/login");
        
        return filter;
    }

    @Bean
    public JWTFilter jwtFilter() {
        return new JWTFilter(jwtUtil);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, LoginFilter loginFilter) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
            .requestMatchers("/auth/login", "/auth/register", "/error", "/favicon.ico").permitAll()
            .requestMatchers("/images/**", "/css/**", "/js/**").permitAll()
            .requestMatchers("/auth/mypage").authenticated()
            //.requestMatchers("/session/list").permitAll()
            .requestMatchers("/", "/session/list").authenticated()
            .anyRequest().authenticated())
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.disable())
            .requestCache((cache) -> cache.disable())
            .formLogin(login -> login.disable())
            .httpBasic(auth -> auth.disable())
            .addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class)
            //.addFilterAfter(jwtFilter(), LoginFilter.class)
            .addFilterBefore(jwtFilter(), LoginFilter.class)
            .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling((exceptionHandling) -> exceptionHandling
            .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/auth/login")))

            .logout((logout) -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/auth/login?logout")
                .addLogoutHandler(jwtLogoutHandler)
                .permitAll()
            );
            
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
    return (web) -> web.ignoring().requestMatchers(
        "/error", // 에러 경로를 완전히 무시
        "/favicon.ico" // (선택 사항) favicon도 확실하게 무시 가능
    );
}
}
