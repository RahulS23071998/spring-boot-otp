package com.starter.springboot.config;

import com.starter.springboot.repository.UserRepository;
import com.starter.springboot.security.jwt.JWTConfigurer;
import com.starter.springboot.security.jwt.ITokenProvider;
import com.starter.springboot.security.OtpAwareAuthenticationProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.data.repository.query.SecurityEvaluationContextExtension;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfiguration {

    private final Http401UnauthorizedEntryPoint authenticationEntryPoint;

    private final UserRepository userRepository;

    private final StringRedisTemplate redisTemplate;

    public SecurityConfiguration(Http401UnauthorizedEntryPoint authenticationEntryPoint,
                                 UserRepository userRepository,
                                 StringRedisTemplate redisTemplate) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
    }

    @Bean
    public MDCFilter mdcFilter() {
        return new MDCFilter();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public OtpAwareAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        return new OtpAwareAuthenticationProvider(userRepository, passwordEncoder, redisTemplate);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ITokenProvider tokenProvider, MDCFilter mdcFilter) throws Exception {
        http
                .exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint))
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/users/public").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/api/greeting/**").authenticated()
                        .requestMatchers("/api/**").authenticated()
                )
                .addFilterBefore(mdcFilter, BasicAuthenticationFilter.class)
                .with(new JWTConfigurer(tokenProvider), Customizer.withDefaults());
        
        return http.build();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityEvaluationContextExtension securityEvaluationContextExtension() {
        return new SecurityEvaluationContextExtension();
    }
}
