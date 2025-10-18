package com.mockerview.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.mockerview.jwt.JWTFilter;
import com.mockerview.jwt.JWTLogoutHandler;
import com.mockerview.jwt.JWTUtil;
import com.mockerview.jwt.LoginFilter;
import com.mockerview.jwt.OAuth2SuccessHandler;
import com.mockerview.repository.UserRepository;
import com.mockerview.service.CustomOAuth2UserService;
import com.mockerview.service.CustomUserDetailsService;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JWTLogoutHandler jwtLogoutHandler;
    private final JWTUtil jwtUtil;
    private final AuthenticationConfiguration authenticationConfiguration;
    private final UserRepository userRepository;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    public SecurityConfig(JWTUtil jwtUtil, 
                        AuthenticationConfiguration authenticationConfiguration,
                        JWTLogoutHandler jwtLogoutHandler,
                        UserRepository userRepository,
                        CustomOAuth2UserService customOAuth2UserService,
                        OAuth2SuccessHandler oAuth2SuccessHandler) throws Exception {
        this.jwtUtil = jwtUtil;
        this.authenticationConfiguration = authenticationConfiguration;
        this.jwtLogoutHandler = jwtLogoutHandler;
        this.userRepository = userRepository;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(
                CustomUserDetailsService customUserDetailsService, 
                PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
                DaoAuthenticationProvider authenticationProvider) throws Exception {
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    public LoginFilter loginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil) throws Exception {
        LoginFilter filter = new LoginFilter(authenticationManager, jwtUtil);
        return filter;
    }

    @Bean
    public JWTFilter jwtFilter() {
        return new JWTFilter(jwtUtil, userRepository);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, LoginFilter loginFilter) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index", "/index.html").permitAll()
                .requestMatchers("/auth/login", "/auth/register", "/auth/find-username", "/auth/reset-password", "/error", "/favicon.ico").permitAll()
                .requestMatchers("/user/login", "/user/register", "/user/loginProc", "/user/registerProc").permitAll()
                .requestMatchers("/images/**", "/css/**", "/js/**", "/*.png", "/*.jpg", "/*.ico").permitAll()
                .requestMatchers("/manifest.json", "/service-worker.js", "/offline.html", "/*.html").permitAll()
                .requestMatchers("/apple-touch-icon**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/api/**").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/dev/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/auth/mypage").authenticated()
                .requestMatchers("/session/**").authenticated()
                .requestMatchers("/payment/**").authenticated()
                .anyRequest().authenticated())
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.disable())
            .requestCache((cache) -> cache.disable())
            .formLogin(login -> login.disable())
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService))
                .successHandler(oAuth2SuccessHandler))
            .httpBasic(auth -> auth.disable())
            .addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtFilter(), LoginFilter.class)
            .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling((exceptionHandling) -> exceptionHandling
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/auth/login")))
            .logout((logout) -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/?logout=success")
                .addLogoutHandler(jwtLogoutHandler)
                .permitAll());
            
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
            "/error",
            "/favicon.ico",
            "/manifest.json",
            "/service-worker.js",
            "/offline.html",
            "/*.png",
            "/*.jpg",
            "/*.ico"
        );
    }
}